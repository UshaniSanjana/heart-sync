package com.heartsync.reporting.controller;

import com.heartsync.reporting.entity.ClinicalReport;
import com.heartsync.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportingService reportingService;

    /**
     * POST /api/reports/generate — manually trigger report generation.
     * ecgRecordId is optional: pass it if the patient has an ECG uploaded.
     * Angiogram results are fetched automatically by patientId if they exist.
     */
    @PostMapping("/generate")
    public ResponseEntity<ClinicalReport> generate(
            @RequestParam String patientId,
            @RequestParam(required = false) String ecgRecordId) {
        return ResponseEntity.ok(reportingService.generateManual(patientId, ecgRecordId));
    }

    /** GET /api/reports/{id} — report metadata */
    @GetMapping("/{id}")
    public ResponseEntity<ClinicalReport> getById(@PathVariable String id) {
        return ResponseEntity.ok(reportingService.getById(id));
    }

    /** GET /api/reports/patient/{patientId} — all reports for a patient */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ClinicalReport>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(reportingService.getByPatient(patientId));
    }

    /**
     * GET /api/reports/{id}/download — streams the PDF file
     * The browser/frontend receives a PDF binary for display.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        byte[] pdf = reportingService.downloadPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"report-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** DELETE /api/reports/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        reportingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
