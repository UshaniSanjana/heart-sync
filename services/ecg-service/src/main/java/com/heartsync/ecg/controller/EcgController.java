package com.heartsync.ecg.controller;

import com.heartsync.ecg.entity.EcgRecord;
import com.heartsync.ecg.service.EcgService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ecg")
@RequiredArgsConstructor
public class EcgController {

    private final EcgService ecgService;

    /**
     * POST /api/ecg/upload
     * Multipart form: file + patientId param.
     * Returns the ECG record with analysis results.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EcgRecord> upload(
            @RequestParam("file")      MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ecgService.upload(patientId, userId, file, traceId));
    }

    /** GET /api/ecg/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<EcgRecord> getById(@PathVariable String id) {
        return ResponseEntity.ok(ecgService.getById(id));
    }

    /** GET /api/ecg/patient/{patientId} */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<EcgRecord>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(ecgService.getByPatient(patientId));
    }

    /** DELETE /api/ecg/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ecgService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
