package com.heartsync.ecg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartsync.ecg.config.RabbitMQConfig;
import com.heartsync.ecg.entity.AiAnalysisOutboxMessage;
import com.heartsync.ecg.entity.EcgRecord;
import com.heartsync.ecg.event.AiAnalysisRequestedEvent;
import com.heartsync.ecg.event.AiAnalysisRequestedEvent.AnalysisType;
import com.heartsync.ecg.repository.AiAnalysisOutboxRepository;
import com.heartsync.ecg.repository.EcgRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcgService {

    private final EcgRecordRepository ecgRecordRepository;
    private final AiAnalysisOutboxRepository outboxRepository;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    @Transactional
    public EcgRecord upload(String patientId, String uploadedBy, MultipartFile file, String traceId) {
        String fileKey = minioStorageService.upload(patientId, file);

        EcgRecord record = EcgRecord.builder()
                .patientId(patientId)
                .uploadedBy(uploadedBy)
                .fileKey(fileKey)
                .fileName(file.getOriginalFilename())
                .status(EcgRecord.AnalysisStatus.ANALYZING)
                .build();
        record = ecgRecordRepository.save(record);

        record = runMockAnalysis(record);
        ecgRecordRepository.save(record);

        String requestId = UUID.randomUUID().toString();
        String resolvedTraceId = traceId != null && !traceId.isBlank() ? traceId : requestId;

        AiAnalysisRequestedEvent event = AiAnalysisRequestedEvent.builder()
                .version(1)
                .requestId(requestId)
                .idempotencyKey("ecg:" + record.getId())
                .traceId(resolvedTraceId)
                .occurredAt(Instant.now())
                .sourceService("ecg-service")
                .analysisType(AnalysisType.ECG)
                .ecgRecordId(record.getId())
                .patientId(patientId)
                .fileKey(record.getFileKey())
                .heartRate(record.getHeartRate())
                .rhythm(record.getRhythm())
                .prInterval(record.getPrInterval())
                .qrsDuration(record.getQrsDuration())
                .qtInterval(record.getQtInterval())
                .findings(record.getFindings())
                .build();

        saveOutboxMessage(event);
        log.info("Saved AI analysis outbox request {} for patient {} ECG {} traceId={}",
                requestId, patientId, record.getId(), resolvedTraceId);

        return record;
    }

    public EcgRecord getById(String id) {
        return ecgRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ECG record not found: " + id));
    }

    public List<EcgRecord> getByPatient(String patientId) {
        return ecgRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public void delete(String id) {
        EcgRecord record = getById(id);
        if (record.getFileKey() != null) {
            try {
                minioStorageService.deleteFile(record.getFileKey());
            } catch (Exception e) {
                log.warn("MinIO delete failed for ECG {}: {}", id, e.getMessage());
            }
        }
        ecgRecordRepository.deleteById(id);
        log.info("Deleted ECG record {}", id);
    }

    private void saveOutboxMessage(AiAnalysisRequestedEvent event) {
        try {
            outboxRepository.save(AiAnalysisOutboxMessage.builder()
                    .requestId(event.getRequestId())
                    .idempotencyKey(event.getIdempotencyKey())
                    .routingKey(RabbitMQConfig.AI_ANALYSIS_ROUTING_KEY)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(AiAnalysisOutboxMessage.Status.PENDING)
                    .attempts(0)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save AI analysis outbox message", e);
        }
    }

    private EcgRecord runMockAnalysis(EcgRecord record) {
        int heartRate = 60 + random.nextInt(40);
        boolean abnormal = random.nextDouble() < 0.3;

        record.setHeartRate(heartRate);
        record.setRhythm(abnormal ? "Irregular rhythm" : "Normal sinus rhythm");
        record.setPrInterval(160 + random.nextInt(40));
        record.setQrsDuration(80 + random.nextInt(20));
        record.setQtInterval(360 + random.nextInt(60));

        if (abnormal) {
            record.setFindings("Possible anteroseptal infarction (old). " +
                    "Ischemic ST-T changes in posterior leads.");
        } else {
            record.setFindings("ECG within normal limits. No significant ST-T changes.");
        }

        record.setStatus(EcgRecord.AnalysisStatus.COMPLETED);
        record.setAnalyzedAt(LocalDateTime.now());
        return record;
    }
}
