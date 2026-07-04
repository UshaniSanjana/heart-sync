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

            PatientData patient = fetchPatient(event.getPatientId());
            String patientName = patientName(event.getPatientId(), patient);
            String dob = patient != null && patient.dateOfBirth() != null
                    ? patient.dateOfBirth() : "N/A";
            String gender = patient != null && patient.gender() != null
                    ? patient.gender() : "N/A";
            String referringPhysician = resolveReferringPhysician(patient);

            // Generate PDF
            PdfGenerationService.ReportData reportData = new PdfGenerationService.ReportData(
                    report.getId(),
                    event.getPatientId(),
                    patientName,
                    dob, gender,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    referringPhysician,
                    clinicalSummary,
                    ecg.heartRate(),
                    ecg.rhythm(),
                    ecg.prInterval(),
                    ecg.qrsDuration(),
                    ecg.qtInterval(),
                    ecg.findings(),
                    ai.coronaryFindings(),
                    ai.estimatedStenosisPercent(),
                    ai.calciumScore(),
                    ai.dominance(),
                    null, null, null, null, null,   // no angiogram data in the legacy flow
                    fusionInsight,
                    ai.overallRisk() != null ? ai.overallRisk() : "LOW",
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

    /**
     * Manually triggered report — includes only the data that actually exists.
     * ECG section appears only if ecgRecordId is provided and data is available.
     * Angiogram section appears only if the patient has a saved angiogram result.
     */
    public ClinicalReport generateManual(String patientId, String ecgRecordId) {
        ClinicalReport report = ClinicalReport.builder()
                .patientId(patientId)
                .ecgRecordId(ecgRecordId)
                .build();
        report = reportRepository.save(report);

        try {
            EcgData ecg = null;
            if (ecgRecordId != null && !ecgRecordId.isBlank()) {
                try {
                    ecg = restTemplate.getForObject(
                            "http://ecg-service/api/ecg/" + ecgRecordId, EcgData.class);
                } catch (Exception e) {
                    log.warn("Could not fetch ECG data for {}: {}", ecgRecordId, e.getMessage());
                }
            }

            AiData ai = null;
            if (ecgRecordId != null && !ecgRecordId.isBlank()) {
                try {
                    ai = restTemplate.getForObject(
                            "http://ai-inference-service/api/ai/results/ecg/" + ecgRecordId, AiData.class);
                } catch (Exception e) {
                    log.warn("Could not fetch AI ECG result for {}: {}", ecgRecordId, e.getMessage());
                }
            }

            AngiogramData angio = null;
            try {
                angio = restTemplate.getForObject(
                        "http://ai-inference-service/api/ai/angiogram/patient/" + patientId, AngiogramData.class);
            } catch (Exception e) {
                log.warn("Could not fetch angiogram result for patient {}: {}", patientId, e.getMessage());
            }

            if (ecg == null && ai == null && angio == null) {
                throw new RuntimeException("No ECG or angiogram data found for this patient — upload data first.");
            }

            String overallRisk    = determineOverallRisk(ai, angio);
            String fusionInsight  = buildManualFusionInsight(ecg, angio);
            String finalImpression = buildFinalImpression(overallRisk);
            String clinicalSummary = buildClinicalSummary(ecg != null, angio != null);

            PatientData patient = fetchPatient(patientId);

            String patientName = patientName(patientId, patient);
            String dob = patient != null && patient.dateOfBirth() != null
                    ? patient.dateOfBirth() : "N/A";
            String gender = patient != null && patient.gender() != null
                    ? patient.gender() : "N/A";
            String referringPhysician = resolveReferringPhysician(patient);

            PdfGenerationService.ReportData reportData = new PdfGenerationService.ReportData(
                    report.getId(),
                    patientId,
                    patientName,
                    dob, gender,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    referringPhysician,
                    clinicalSummary,
                    ecg != null ? ecg.heartRate() : null,
                    ecg != null ? ecg.rhythm()    : null,
                    ecg != null ? ecg.prInterval()  : null,
                    ecg != null ? ecg.qrsDuration() : null,
                    ecg != null ? ecg.qtInterval()  : null,
                    ecg != null ? ecg.findings()    : null,
                    ai  != null ? ai.coronaryFindings() : null,
                    ai  != null ? ai.estimatedStenosisPercent() : null,
                    ai  != null ? ai.calciumScore() : null,
                    ai  != null ? ai.dominance()    : null,
                    angio != null ? angio.overallRisk()              : null,
                    angio != null ? angio.confidence()               : null,
                    angio != null ? angio.totalBranches()            : null,
                    angio != null ? angio.estimatedStenosisPercent() : null,
                    angio != null ? angio.lesions()                  : null,
                    fusionInsight,
                    overallRisk,
                    finalImpression
            );

            byte[] pdfBytes = pdfGenerationService.generate(reportData);
            String pdfKey   = minioStorageService.uploadPdf(report.getId(), pdfBytes);

            report.setStatus(ClinicalReport.ReportStatus.COMPLETED);
            report.setPdfKey(pdfKey);
            report.setClinicalSummary(clinicalSummary);
            report.setFinalImpression(finalImpression);
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);
            log.info("Manual report {} generated for patient {}", report.getId(), patientId);
            return report;

        } catch (Exception e) {
            log.error("Manual report generation failed for {}", report.getId(), e);
            report.setStatus(ClinicalReport.ReportStatus.FAILED);
            reportRepository.save(report);
            return report;
        }
    }

    private String determineOverallRisk(AiData ai, AngiogramData angio) {
        String aiRisk    = ai    != null ? ai.overallRisk()    : null;
        String angioRisk = angio != null ? angio.overallRisk() : null;
        if ("HIGH".equals(aiRisk) || "HIGH".equals(angioRisk))     return "HIGH";
        if ("MODERATE".equals(aiRisk) || "MODERATE".equals(angioRisk)) return "MODERATE";
        if (aiRisk != null || angioRisk != null)                    return "LOW";
        return "LOW";
    }

    private String buildManualFusionInsight(EcgData ecg, AngiogramData angio) {
        if (ecg == null && angio == null) return "No data available.";
        if (ecg == null)  return "Angiogram-only report. ECG data not available.";
        if (angio == null) return "ECG-only report. Angiogram data not available.";
        StringBuilder sb = new StringBuilder("Multimodal fusion of ECG and angiogram findings:\n");
        if (ecg.findings() != null && ecg.findings().contains("infarction"))
            sb.append("• ECG suggests possible prior ischemic event\n");
        else
            sb.append("• ECG within normal limits\n");
        sb.append("• Angiogram overall risk: ").append(angio.overallRisk()).append("\n");
        return sb.toString();
    }

    private String buildClinicalSummary(boolean hasEcg, boolean hasAngio) {
        if (hasEcg && hasAngio)
            return "Multimodal cardiovascular evaluation combining ECG and coronary angiography findings.";
        if (hasEcg)
            return "ECG-based cardiac evaluation. Coronary angiography not performed.";
        return "Coronary angiography evaluation. ECG data not included in this report.";
    }

    private PatientData fetchPatient(String patientId) {
        try {
            return restTemplate.getForObject(
                    "http://patient-service/api/patients/" + patientId, PatientData.class);
        } catch (Exception e) {
            log.warn("Could not fetch patient data for {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    private String patientName(String patientId, PatientData patient) {
        if (patient == null) return "Patient " + patientId.substring(0, 8);
        String name = ((patient.firstName() != null ? patient.firstName() : "") + " " +
                (patient.lastName() != null ? patient.lastName() : "")).trim();
        return !name.isBlank() ? name : "Patient " + patientId.substring(0, 8);
    }

    private String resolveReferringPhysician(PatientData patient) {
        if (patient == null) return "N/A";
        if (patient.referringPhysician() != null && !patient.referringPhysician().isBlank()) {
            return patient.referringPhysician();
        }
        if (patient.registeredBy() != null && !patient.registeredBy().isBlank()) {
            try {
                UserData user = restTemplate.getForObject(
                        "http://iam-service/api/internal/users/" + patient.registeredBy(), UserData.class);
                if (user != null && user.fullName() != null && !user.fullName().isBlank()) {
                    return formatDoctorName(user.fullName(), user.role());
                }
            } catch (Exception e) {
                log.warn("Could not fetch registering doctor {}: {}", patient.registeredBy(), e.getMessage());
            }
        }
        return "N/A";
    }

    private String formatDoctorName(String fullName, String role) {
        String name = fullName.trim();
        if (name.toLowerCase().startsWith("dr.")) return name;
        return "DOCTOR".equals(role) ? "Dr. " + name : name;
    }

    record LesionData(int rank, double dsPercent, String severity,
                      double mldPx, double rvdPx, double lengthPx) {}

    record AngiogramData(String id, String patientId, String overallRisk,
                         Double confidence, Integer totalBranches, Boolean calibrated,
                         Integer estimatedStenosisPercent, java.util.List<LesionData> lesions) {}

    record PatientData(String id, String firstName, String lastName,
                       String dateOfBirth, String gender, String referringPhysician,
                       String registeredBy) {}

    record UserData(String id, String email, String fullName, String role) {}

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

    public void delete(String id) {
        ClinicalReport report = getById(id);
        if (report.getPdfKey() != null) {
            try { minioStorageService.deletePdf(report.getPdfKey()); }
            catch (Exception e) { log.warn("MinIO delete failed for report {}: {}", id, e.getMessage()); }
        }
        reportRepository.deleteById(id);
        log.info("Deleted report {}", id);
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
