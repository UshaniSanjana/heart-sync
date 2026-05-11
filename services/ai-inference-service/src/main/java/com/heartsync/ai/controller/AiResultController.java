package com.heartsync.ai.controller;

import com.heartsync.ai.document.AnalysisResult;
import com.heartsync.ai.service.AiInferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiResultController {

    private final AiInferenceService aiInferenceService;

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
