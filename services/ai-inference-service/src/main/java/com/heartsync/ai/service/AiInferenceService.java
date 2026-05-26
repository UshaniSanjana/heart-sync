package com.heartsync.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartsync.ai.config.RabbitMQConfig;
import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.event.AiCompletedEvent;
import com.heartsync.ai.event.EcgAnalyzedEvent;
import com.heartsync.ai.repository.AnalysisResultRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInferenceService {

    private final AnalysisResultRepository repository;
    private final RabbitTemplate           rabbitTemplate;
    private final MinioClient              minioClient;
    private final ObjectMapper             objectMapper;

    @Value("${minio.bucket:ecg-files}")
    private String minioBucket;

    @Value("${ecg-model.service-url:http://localhost:8086}")
    private String modelServiceUrl;

    /**
     * Label-to-risk mapping for the Swin ECG classifier output.
     * Maps each classification code to a clinical risk level.
     */
    private static final Map<String, String> RISK_MAP = Map.of(
            "N", "LOW",        // Normal beat
            "S", "MODERATE",   // Supraventricular premature beat
            "V", "HIGH",       // Premature ventricular contraction
            "F", "MODERATE",   // Fusion of ventricular and normal beat
            "Q", "MODERATE",   // Unclassifiable beat
            "M", "HIGH"        // Myocardial Infarction
    );

    /**
     * Label-to-finding mapping for clinical report generation.
     */
    private static final Map<String, String> FINDING_MAP = Map.of(
            "N", "Normal ECG — no significant abnormalities detected",
            "S", "Supraventricular premature beat detected — may indicate atrial irritability",
            "V", "Premature ventricular contraction detected — potential ventricular arrhythmia risk",
            "F", "Fusion beat detected — overlap of ventricular and normal conduction",
            "Q", "Unclassifiable ECG pattern — manual clinical review recommended",
            "M", "Possible myocardial infarction detected — urgent cardiology consultation recommended"
    );

    /**
     * Triggered by EcgAnalyzedEvent from RabbitMQ.
     * Downloads ECG image from MinIO, sends to the Python model sidecar,
     * stores results in MongoDB, then publishes AiCompletedEvent.
     */
    public void analyze(EcgAnalyzedEvent event) {
        log.info("AI analysis started for ECG record {}", event.getEcgRecordId());

        AnalysisResult result;

        try {
            // Step 1: Download ECG image from MinIO
            byte[] imageBytes = downloadFromMinio(event.getFileKey());
            log.info("Downloaded ECG image from MinIO: {} ({} bytes)", event.getFileKey(), imageBytes.length);

            // Step 2: Send to Python model sidecar for classification
            JsonNode modelResponse = callModelService(imageBytes, event.getFileKey());
            log.info("Model prediction for ECG {}: {}", event.getEcgRecordId(), modelResponse);

            // Step 3: Parse response and build AnalysisResult
            result = buildResultFromModelResponse(event, modelResponse);

        } catch (Exception e) {
            log.error("Model inference failed for ECG record {}, falling back to event-based analysis",
                    event.getEcgRecordId(), e);
            // Fallback: use event data if model service is unavailable
            result = buildFallbackResult(event);
        }

        // Step 4: Save to MongoDB
        repository.save(result);

        // Step 5: Publish completion event for Reporting Service
        AiCompletedEvent completedEvent = AiCompletedEvent.builder()
                .analysisResultId(result.getId())
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .overallRisk(result.getOverallRisk())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.AI_ROUTING_KEY, completedEvent);
        log.info("Published AiCompletedEvent for patient {} analysis {}", event.getPatientId(), result.getId());
    }

    public AnalysisResult getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis result not found: " + id));
    }

    public AnalysisResult getByEcgRecord(String ecgRecordId) {
        return repository.findByEcgRecordId(ecgRecordId)
                .orElseThrow(() -> new IllegalArgumentException("No AI result for ECG record: " + ecgRecordId));
    }

    public List<AnalysisResult> getByPatient(String patientId) {
        return repository.findByPatientIdOrderByAnalyzedAtDesc(patientId);
    }

    // ─── Private Helpers ──────────────────────────────────────────

    /**
     * Downloads the ECG image file from MinIO using the object key.
     */
    private byte[] downloadFromMinio(String fileKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(fileKey)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download ECG image from MinIO: " + fileKey, e);
        }
    }

    /**
     * Sends the ECG image to the Python model sidecar (POST /predict)
     * and returns the parsed JSON response.
     */
    private JsonNode callModelService(byte[] imageBytes, String fileName) {
        WebClient client = WebClient.builder()
                .baseUrl(modelServiceUrl)
                .build();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return fileName != null ? fileName : "ecg_image.png";
            }
        }).contentType(MediaType.IMAGE_PNG);

        String responseBody = client.post()
                .uri("/predict")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse model response: " + responseBody, e);
        }
    }

    /**
     * Builds an AnalysisResult from the Python model sidecar's JSON response.
     *
     * Expected response format:
     * {
     *   "prediction": "N",
     *   "label": "Normal beat",
     *   "confidence": 0.94,
     *   "all_predictions": {"N": 0.94, "S": 0.02, "V": 0.01, ...}
     * }
     */
    private AnalysisResult buildResultFromModelResponse(EcgAnalyzedEvent event, JsonNode response) {
        String prediction = response.get("prediction").asText();
        String label = response.get("label").asText();
        double confidence = response.get("confidence").asDouble();

        // Parse all_predictions map
        Map<String, Double> allPredictions = new LinkedHashMap<>();
        JsonNode allPreds = response.get("all_predictions");
        if (allPreds != null) {
            allPreds.fields().forEachRemaining(entry ->
                    allPredictions.put(entry.getKey(), entry.getValue().asDouble()));
        }

        String risk = RISK_MAP.getOrDefault(prediction, "MODERATE");
        String finding = FINDING_MAP.getOrDefault(prediction, "ECG analysis completed");

        // Build coronary findings based on prediction
        Map<String, String> coronaryFindings = new LinkedHashMap<>();
        coronaryFindings.put("ECG Classification", label);
        coronaryFindings.put("Clinical Finding", finding);
        coronaryFindings.put("Model Confidence", String.format("%.1f%%", confidence * 100));

        return AnalysisResult.builder()
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .prediction(prediction)
                .predictionLabel(label)
                .confidence(confidence)
                .allPredictions(allPredictions)
                .coronaryFindings(coronaryFindings)
                .overallRisk(risk)
                .rawModelOutput(response.toString())
                .build();
    }

    /**
     * Fallback analysis when the model service is unavailable.
     * Uses data from the ECG service's event to produce a basic result.
     */
    private AnalysisResult buildFallbackResult(EcgAnalyzedEvent event) {
        log.warn("Using fallback analysis for ECG record {}", event.getEcgRecordId());

        boolean hasAbnormalEcg = event.getFindings() != null &&
                event.getFindings().contains("infarction");

        String risk = hasAbnormalEcg ? "HIGH" : "LOW";
        String prediction = hasAbnormalEcg ? "M" : "N";
        String label = hasAbnormalEcg ? "Possible Myocardial Infarction (fallback)" : "Normal (fallback)";

        Map<String, String> coronaryFindings = new LinkedHashMap<>();
        coronaryFindings.put("ECG Classification", label);
        coronaryFindings.put("Clinical Finding", event.getFindings() != null ? event.getFindings() : "No findings available");
        coronaryFindings.put("Note", "Analysis performed using fallback — model service was unavailable");

        return AnalysisResult.builder()
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .prediction(prediction)
                .predictionLabel(label)
                .confidence(0.0)
                .coronaryFindings(coronaryFindings)
                .overallRisk(risk)
                .rawModelOutput("{\"model\":\"fallback\",\"reason\":\"model-service-unavailable\"}")
                .build();
    }
}
