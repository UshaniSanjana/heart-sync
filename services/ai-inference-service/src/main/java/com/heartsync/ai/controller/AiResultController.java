package com.heartsync.ai.controller;

import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.document.AngiogramResult;
import com.heartsync.ai.dto.AngiogramAnalysisAcceptedResponse;
import com.heartsync.ai.service.AiInferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiResultController {

    private final AiInferenceService aiInferenceService;

    @PostMapping(value = "/angiogram/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AngiogramAnalysisAcceptedResponse> analyzeAngiogram(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) throws Exception {
        return ResponseEntity.accepted().body(aiInferenceService.requestAngiogramAnalysis(patientId, file, traceId));
    }

    @GetMapping("/angiogram/patient/{patientId}")
    public ResponseEntity<AngiogramResult> getAngiogramByPatient(@PathVariable String patientId) {
        AngiogramResult result = aiInferenceService.getLatestAngiogramResult(patientId);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<AnalysisResult> getById(@PathVariable String id) {
        return ResponseEntity.ok(aiInferenceService.getById(id));
    }

    @GetMapping("/results/ecg/{ecgRecordId}")
    public ResponseEntity<AnalysisResult> getByEcgRecord(@PathVariable String ecgRecordId) {
        return ResponseEntity.ok(aiInferenceService.getByEcgRecord(ecgRecordId));
    }

    @GetMapping("/results/patient/{patientId}")
    public ResponseEntity<List<AnalysisResult>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(aiInferenceService.getByPatient(patientId));
    }
}
