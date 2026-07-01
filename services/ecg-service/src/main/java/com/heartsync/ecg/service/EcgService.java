package com.heartsync.ecg.service;

import com.heartsync.ecg.config.RabbitMQConfig;
import com.heartsync.ecg.entity.EcgRecord;
import com.heartsync.ecg.event.EcgAnalyzedEvent;
import com.heartsync.ecg.repository.EcgRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcgService {

    private final EcgRecordRepository ecgRecordRepository;
    private final MinioStorageService  minioStorageService;
    private final RabbitTemplate       rabbitTemplate;

    private final Random random = new Random();

    /**
     * Full upload pipeline:
     *  1. Upload file to MinIO
     *  2. Save record to PostgreSQL with ANALYZING status
     *  3. Run mock ECG analysis (simulates ML model output)
     *  4. Update record with results + COMPLETED status
     *  5. Publish EcgAnalyzedEvent to RabbitMQ → triggers AI pipeline
     */
    public EcgRecord upload(String patientId, String uploadedBy, MultipartFile file) {
        // Step 1: Store file
        String fileKey = minioStorageService.upload(patientId, file);

        // Step 2: Create DB record
        EcgRecord record = EcgRecord.builder()
                .patientId(patientId)
                .uploadedBy(uploadedBy)
                .fileKey(fileKey)
                .fileName(file.getOriginalFilename())
                .status(EcgRecord.AnalysisStatus.ANALYZING)
                .build();
        record = ecgRecordRepository.save(record);

        // Step 3 & 4: Mock analysis
        record = runMockAnalysis(record);
        ecgRecordRepository.save(record);

        // Step 5: Publish event — triggers AI service pipeline
        EcgAnalyzedEvent event = EcgAnalyzedEvent.builder()
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

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ECG_ROUTING_KEY, event);
        log.info("Published EcgAnalyzedEvent for patient {} record {}", patientId, record.getId());

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
            try { minioStorageService.deleteFile(record.getFileKey()); }
            catch (Exception e) { log.warn("MinIO delete failed for ECG {}: {}", id, e.getMessage()); }
        }
        ecgRecordRepository.deleteById(id);
        log.info("Deleted ECG record {}", id);
    }

    /**
     * Generates realistic ECG values.
     * In production this calls a real ML inference endpoint.
     */
    private EcgRecord runMockAnalysis(EcgRecord record) {
        int heartRate = 60 + random.nextInt(40);   // 60–100 bpm
        boolean abnormal = random.nextDouble() < 0.3; // 30% chance of abnormal finding

        record.setHeartRate(heartRate);
        record.setRhythm(abnormal ? "Irregular rhythm" : "Normal sinus rhythm");
        record.setPrInterval(160 + random.nextInt(40));   // 160–200 ms
        record.setQrsDuration(80  + random.nextInt(20));  // 80–100 ms
        record.setQtInterval(360  + random.nextInt(60));  // 360–420 ms

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
