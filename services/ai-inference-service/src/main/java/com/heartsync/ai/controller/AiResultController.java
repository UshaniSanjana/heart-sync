package com.heartsync.ai.controller;

import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.document.AngiogramResult;
import com.heartsync.ai.dto.AngiogramAnalysisResponse;
import com.heartsync.ai.service.AiInferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiResultController {

    private final AiInferenceService aiInferenceService;

    /** POST /api/ai/angiogram/analyze — segmentation + QCA, saves result to MongoDB */
    @PostMapping(value = "/angiogram/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AngiogramAnalysisResponse> analyzeAngiogram(
            @RequestParam("file")      MultipartFile file,
            @RequestParam("patientId") String patientId) throws Exception {
        return ResponseEntity.ok(aiInferenceService.analyzeAndSaveAngiogram(patientId, file.getBytes()));
    }

    /** GET /api/ai/angiogram/patient/{patientId} — latest saved angiogram result */
    @GetMapping("/angiogram/patient/{patientId}")
    public ResponseEntity<AngiogramResult> getAngiogramByPatient(@PathVariable String patientId) {
        AngiogramResult result = aiInferenceService.getLatestAngiogramResult(patientId);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    /** GET /api/ai/results/{id} */
    @GetMapping("/results/{id}")
    public ResponseEntity<AnalysisResult> getById(@PathVariable String id) {
        return ResponseEntity.ok(aiInferenceService.getById(id));
    }

    /** GET /api/ai/results/ecg/{ecgRecordId} */
    @GetMapping("/results/ecg/{ecgRecordId}")
    public ResponseEntity<AnalysisResult> getByEcgRecord(@PathVariable String ecgRecordId) {
        return ResponseEntity.ok(aiInferenceService.getByEcgRecord(ecgRecordId));
    }

    /** GET /api/ai/results/patient/{patientId} */
    @GetMapping("/results/patient/{patientId}")
    public ResponseEntity<List<AnalysisResult>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(aiInferenceService.getByPatient(patientId));
    }
}
