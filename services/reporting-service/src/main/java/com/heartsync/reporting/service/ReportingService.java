package com.heartsync.reporting.service;

import com.heartsync.reporting.entity.ClinicalReport;
import com.heartsync.reporting.event.AiCompletedEvent;
import com.heartsync.reporting.repository.ClinicalReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final ClinicalReportRepository reportRepository;
    private final PdfGenerationService     pdfGenerationService;
    private final MinioStorageService      minioStorageService;
    private final RestTemplate             restTemplate;  // @LoadBalanced — resolves via Eureka

    /**
     * Full report generation pipeline triggered by AiCompletedEvent:
     *  1. Create a GENERATING report record
     *  2. Fetch ECG data from ecg-service via REST (Eureka-resolved)
     *  3. Fetch AI data from ai-inference-service via REST (Eureka-resolved)
     *  4. Build the PDF
     *  5. Store PDF in MinIO
     *  6. Update report record to COMPLETED
     */
    public void generateReport(AiCompletedEvent event) {
        log.info("Starting report generation for patient {} ECG {}", event.getPatientId(), event.getEcgRecordId());

        ClinicalReport report = ClinicalReport.builder()
                .patientId(event.getPatientId())
                .ecgRecordId(event.getEcgRecordId())
                .aiAnalysisId(event.getAnalysisResultId())
                .overallRisk(event.getOverallRisk())
                .build();
        report = reportRepository.save(report);

        try {
            // Fetch ECG results — RestTemplate uses lb://ecg-service which Eureka resolves
            EcgData ecg = restTemplate.getForObject(
                    "http://ecg-service/api/ecg/" + event.getEcgRecordId(), EcgData.class);

            // Fetch AI results
            AiData ai = restTemplate.getForObject(
                    "http://ai-inference-service/api/ai/results/" + event.getAnalysisResultId(), AiData.class);

            if (ecg == null || ai == null) {
                throw new RuntimeException("Failed to fetch ECG or AI data");
            }

            // Build fusion insight text based on combined data
            String fusionInsight = buildFusionInsight(ecg, ai);
            String finalImpression = buildFinalImpression(ai.overallRisk());
            String clinicalSummary = "Patient was evaluated for cardiovascular symptoms. " +
                    "Multimodal analysis combining ECG and coronary angiography findings was performed.";

            // Generate PDF
            PdfGenerationService.ReportData reportData = new PdfGenerationService.ReportData(
                    report.getId(),
                    event.getPatientId(),
                    "Patient " + event.getPatientId().substring(0, 8),  // placeholder — patient service has full name
                    "N/A",
                    "N/A",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    "Dr. HeartSync",
                    clinicalSummary,
                    ecg.heartRate() != null ? ecg.heartRate() : 72,
                    ecg.rhythm()    != null ? ecg.rhythm()    : "Normal sinus rhythm",
                    ecg.prInterval()  != null ? ecg.prInterval()  : 180,
                    ecg.qrsDuration() != null ? ecg.qrsDuration() : 90,
                    ecg.qtInterval()  != null ? ecg.qtInterval()  : 400,
                    ai.coronaryFindings() != null ? ai.coronaryFindings() : Map.of("LMA", "Normal"),
                    ai.estimatedStenosisPercent() != null ? ai.estimatedStenosisPercent() : 0,
                    ai.calciumScore()  != null ? ai.calciumScore()  : "Normal",
                    ai.dominance()     != null ? ai.dominance()     : "Right",
                    fusionInsight,
                    ai.overallRisk()   != null ? ai.overallRisk()   : "LOW",
                    finalImpression
            );

            byte[] pdfBytes = pdfGenerationService.generate(reportData);
            String pdfKey   = minioStorageService.uploadPdf(report.getId(), pdfBytes);

            // Update report to COMPLETED
            report.setStatus(ClinicalReport.ReportStatus.COMPLETED);
            report.setPdfKey(pdfKey);
            report.setClinicalSummary(clinicalSummary);
            report.setFinalImpression(finalImpression);
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);

            log.info("Report {} generated and stored at {}", report.getId(), pdfKey);

        } catch (Exception e) {
            log.error("Report generation failed for {}", report.getId(), e);
            report.setStatus(ClinicalReport.ReportStatus.FAILED);
            reportRepository.save(report);
        }
    }

    public ClinicalReport getById(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));
    }

    public List<ClinicalReport> getByPatient(String patientId) {
        return reportRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public byte[] downloadPdf(String id) {
        ClinicalReport report = getById(id);
        if (report.getPdfKey() == null) {
            throw new IllegalStateException("Report PDF not ready yet");
        }
        return minioStorageService.downloadPdf(report.getPdfKey());
    }

    private String buildFusionInsight(EcgData ecg, AiData ai) {
        StringBuilder sb = new StringBuilder("By combining ECG abnormalities with angiogram findings:\n");
        if (ecg.findings() != null && ecg.findings().contains("infarction")) {
            sb.append("• ECG suggests possible prior ischemic event\n");
        } else {
            sb.append("• ECG is within normal limits\n");
        }
        if (ai.estimatedStenosisPercent() != null && ai.estimatedStenosisPercent() >= 50) {
            sb.append("• Angiogram shows significant coronary stenosis requiring further evaluation\n");
        } else {
            sb.append("• Angiogram shows no significant coronary obstruction\n");
        }
        return sb.toString();
    }

    private String buildFinalImpression(String risk) {
        return switch (risk) {
            case "HIGH"     -> "Urgent cardiology referral recommended.\nConsider coronary intervention.\nClose monitoring required.";
            case "MODERATE" -> "Continue rest and limited physical activity.\nSchedule follow-up ECG in 2-4 weeks.\nConsider further evaluation if symptoms persist.";
            default         -> "No significant cardiovascular findings.\nRoutine monitoring recommended.\nMaintain healthy lifestyle.";
        };
    }

    // Simple response DTOs for inter-service REST calls
    record EcgData(String id, String patientId, Integer heartRate, String rhythm,
                   Integer prInterval, Integer qrsDuration, Integer qtInterval, String findings) {}

    @SuppressWarnings("unchecked")
    record AiData(String id, String patientId, Map<String, String> coronaryFindings,
                  Integer estimatedStenosisPercent, String calciumScore,
                  String dominance, String overallRisk) {}
}
