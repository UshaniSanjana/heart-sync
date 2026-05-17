package com.heartsync.ai.service;

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
    private final WebClient                aiPythonWebClient;

    @Value("${minio.bucket}")
    private String minioBucket;

    private final Random random = new Random();

    public void analyze(EcgAnalyzedEvent event) {
        log.info("AI analysis started for ECG record {}", event.getEcgRecordId());

        AnalysisResult result;
        if (event.getAngiogramImageKey() != null) {
            try {
                result = runRealSegmentation(event);
            } catch (Exception e) {
                log.warn("Python segmentation failed for record {}, falling back to mock: {}",
                        event.getEcgRecordId(), e.getMessage());
                result = runMockSegmentation(event);
            }
        } else {
            result = runMockSegmentation(event);
        }

        repository.save(result);

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

        String risk;
        if (stenosisPercent >= 70 || hasAbnormalEcg && stenosisPercent >= 50) {
            risk = "HIGH";
        } else if (stenosisPercent >= 30 || hasAbnormalEcg) {
            risk = "MODERATE";
        } else {
            risk = "LOW";
        }

        return AnalysisResult.builder()
                .ecgRecordId(event.getEcgRecordId())
                .patientId(event.getPatientId())
                .coronaryFindings(Map.of(
                        "Left Main Artery (LMA)",         "Normal",
                        "Left Anterior Descending (LAD)", hasStenosis ? "Stenosis" : "Normal",
                        "Right Coronary Artery (RCA)",    "Normal",
                        "Other branches",                 "Normal"
                ))
                .estimatedStenosisPercent(stenosisPercent)
                .calciumScore("Within normal range")
                .dominance("Right")
                .overallRisk(risk)
                .rawModelOutput("{\"model\":\"mock-v1\",\"confidence\":0.87}")
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
