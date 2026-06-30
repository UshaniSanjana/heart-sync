package com.heartsync.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartsync.ai.config.RabbitMQConfig;
import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.dto.AngiogramAnalysisResponse;
import com.heartsync.ai.dto.QcaRequest;
import com.heartsync.ai.dto.QcaResponse;
import com.heartsync.ai.dto.SegmentationRequest;
import com.heartsync.ai.dto.SegmentationResponse;
import com.heartsync.ai.event.AiCompletedEvent;
import com.heartsync.ai.event.EcgAnalyzedEvent;
import com.heartsync.ai.repository.AnalysisResultRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;

import io.minio.PutObjectArgs;

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

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AiInferenceService {

    private final AnalysisResultRepository repository;
    private final RabbitTemplate           rabbitTemplate;
    private final MinioClient              minioClient;
    private final ObjectMapper             objectMapper;
    private final WebClient                aiPythonWebClient;

    private final Random random = new Random();


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
    public AngiogramAnalysisResponse analyzeAngiogram(byte[] imageBytes) {
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        long t0 = System.currentTimeMillis();
        SegmentationResponse segResp = aiPythonWebClient.post()
                .uri("/segment")
                .bodyValue(new SegmentationRequest(imageBase64, "direct", "direct"))
                .retrieve()
                .bodyToMono(SegmentationResponse.class)
                .block();
        int segMs = (int) (System.currentTimeMillis() - t0);

        long t1 = System.currentTimeMillis();
        QcaResponse qcaResp = aiPythonWebClient.post()
                .uri("/qca")
                .bodyValue(new QcaRequest(segResp.getMaskBase64(), imageBase64, "direct", "direct", null))
                .retrieve()
                .bodyToMono(QcaResponse.class)
                .block();
        int qcaMs = (int) (System.currentTimeMillis() - t1);

        List<AngiogramAnalysisResponse.LesionItem> lesions = qcaResp.getLesions().stream()
                .map(l -> AngiogramAnalysisResponse.LesionItem.builder()
                        .rank(l.getRank())
                        .dsPercent(l.getDsPercent())
                        .severity(l.getSeverity())
                        .mldPx(l.getMldPx())
                        .rvdPx(l.getRvdPx())
                        .lengthPx(l.getLengthPx())
                        .mldMm(l.getMldMm())
                        .rvdMm(l.getRvdMm())
                        .lengthMm(l.getLengthMm())
                        .narrowestPoint(l.getNarrowestPoint())
                        .build())
                .collect(Collectors.toList());

        return AngiogramAnalysisResponse.builder()
                .overlayBase64(qcaResp.getOverlayBase64())
                .maskBase64(segResp.getMaskBase64())
                .confidence(segResp.getConfidence())
                .overallRisk(qcaResp.getOverallRisk())
                .totalBranches(qcaResp.getTotalBranches())
                .calibrated(qcaResp.isCalibrated())
                .lesions(lesions)
                .segmentationTimeMs(segMs)
                .qcaTimeMs(qcaMs)
                .build();
    }

    private AnalysisResult runRealSegmentation(EcgAnalyzedEvent event) {
        byte[] imageBytes = fetchFromMinio(event.getAngiogramImageKey());
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        SegmentationResponse segResp = aiPythonWebClient.post()
                .uri("/segment")
                .bodyValue(new SegmentationRequest(imageBase64, event.getPatientId(), event.getEcgRecordId()))
                .retrieve()
                .bodyToMono(SegmentationResponse.class)
                .block();

        QcaResponse qcaResp = aiPythonWebClient.post()
                .uri("/qca")
                .bodyValue(new QcaRequest(segResp.getMaskBase64(), imageBase64,
                        event.getPatientId(), event.getEcgRecordId(), null))
                .retrieve()
                .bodyToMono(QcaResponse.class)
                .block();

        String maskKey = "masks/" + event.getEcgRecordId() + "_mask.png";
        uploadToMinio(maskKey, Base64.getDecoder().decode(segResp.getMaskBase64()), "image/png");

        String overlayKey = null;
        if (qcaResp.getOverlayBase64() != null) {
            overlayKey = "overlays/" + event.getEcgRecordId() + "_overlay.png";
            uploadToMinio(overlayKey, Base64.getDecoder().decode(qcaResp.getOverlayBase64()), "image/png");
        }

        List<AnalysisResult.LesionResult> lesions = qcaResp.getLesions().stream()
                .map(l -> AnalysisResult.LesionResult.builder()
                        .rank(l.getRank())
                        .dsPercent(l.getDsPercent())
                        .severity(l.getSeverity())
                        .mldPx(l.getMldPx())
                        .rvdPx(l.getRvdPx())
                        .lengthPx(l.getLengthPx())
                        .mldMm(l.getMldMm())
                        .rvdMm(l.getRvdMm())
                        .lengthMm(l.getLengthMm())
                        .narrowestPoint(l.getNarrowestPoint())
                        .build())
                .collect(Collectors.toList());

        int maxDs = lesions.stream()
                .mapToInt(l -> (int) Math.round(l.getDsPercent()))
                .max().orElse(0);

        String ladStatus = maxDs >= 70 ? "Occlusion" : maxDs >= 50 ? "Stenosis" : maxDs >= 30 ? "Moderate Stenosis" : "Normal";

        return AnalysisResult.builder()
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .coronaryFindings(Map.of(
                        "Left Main Artery (LMA)",         "Normal",
                        "Left Anterior Descending (LAD)", ladStatus,
                        "Right Coronary Artery (RCA)",    "Normal",
                        "Other branches",                 "Normal"
                ))
                .estimatedStenosisPercent(maxDs)
                .calciumScore("Within normal range")
                .dominance("Right")
                .overallRisk(qcaResp.getOverallRisk())
                .rawModelOutput("{\"model\":\"mobileunetv3\",\"confidence\":" + segResp.getConfidence() + "}")
                .lesions(lesions)
                .totalBranches(qcaResp.getTotalBranches())
                .calibrated(qcaResp.isCalibrated())
                .angiogramImageKey(event.getAngiogramImageKey())
                .segmentationMaskKey(maskKey)
                .overlayImageKey(overlayKey)
                .build();
    }

    private AnalysisResult runMockSegmentation(EcgAnalyzedEvent event) {
        boolean hasAbnormalEcg = event.getFindings() != null &&
                event.getFindings().contains("infarction");

        double stenosisChance = hasAbnormalEcg ? 0.4 : 0.15;
        boolean hasStenosis = random.nextDouble() < stenosisChance;
        int stenosisPercent = hasStenosis ? 20 + random.nextInt(60) : random.nextInt(20);

        String risk = hasAbnormalEcg ? "HIGH" : (hasStenosis ? "MODERATE" : "LOW");

        return AnalysisResult.builder()
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .estimatedStenosisPercent(stenosisPercent)
                .calciumScore("Within normal range")
                .dominance("Right")
                .overallRisk(risk)
                .coronaryFindings(Map.of(
                        "Left Main Artery (LMA)",         "Normal",
                        "Left Anterior Descending (LAD)", hasStenosis ? "Stenosis" : "Normal",
                        "Right Coronary Artery (RCA)",    "Normal",
                        "Other branches",                 "Normal"
                ))
                .rawModelOutput("{\"model\":\"mock\",\"stenosisPercent\":" + stenosisPercent + "}")
                .build();
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
                .calciumScore("Within normal range")
                .dominance("Right")
                .overallRisk(risk)
                .rawModelOutput("{\"model\":\"fallback\",\"reason\":\"model-service-unavailable\"}")
                .build();
    }

    private byte[] fetchFromMinio(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch from MinIO: " + objectKey, e);
        }
    }

    private void uploadToMinio(String objectKey, byte[] data, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO: " + objectKey, e);
        }
    }
}
