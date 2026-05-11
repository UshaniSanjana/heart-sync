package com.heartsync.ai.service;

import com.heartsync.ai.config.RabbitMQConfig;
import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.event.AiCompletedEvent;
import com.heartsync.ai.event.EcgAnalyzedEvent;
import com.heartsync.ai.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInferenceService {

    private final AnalysisResultRepository repository;
    private final RabbitTemplate           rabbitTemplate;

    private final Random random = new Random();

    /**
     * Triggered by EcgAnalyzedEvent from RabbitMQ.
     * Runs mock coronary segmentation analysis, stores in MongoDB,
     * then publishes AiCompletedEvent for Reporting Service.
     */
    public void analyze(EcgAnalyzedEvent event) {
        log.info("AI analysis started for ECG record {}", event.getEcgRecordId());

        AnalysisResult result = runMockSegmentation(event);
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

    /**
     * Mock coronary segmentation.
     * In production: calls a Python FastAPI ML service or a cloud Vision API.
     * Results match the CardioSync report format shown in the presentation.
     */
    private AnalysisResult runMockSegmentation(EcgAnalyzedEvent event) {
        boolean hasAbnormalEcg = event.getFindings() != null &&
                event.getFindings().contains("infarction");

        // If ECG shows possible infarction, slightly increase chance of coronary finding
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
                        "Left Main Artery (LMA)",        "Normal",
                        "Left Anterior Descending (LAD)", hasStenosis ? "Stenosis" : "Normal",
                        "Right Coronary Artery (RCA)",   "Normal",
                        "Other branches",                "Normal"
                ))
                .estimatedStenosisPercent(stenosisPercent)
                .calciumScore("Within normal range")
                .dominance("Right")
                .overallRisk(risk)
                .rawModelOutput("{\"model\":\"mock-v1\",\"confidence\":0.87}")
                .build();
    }
}
