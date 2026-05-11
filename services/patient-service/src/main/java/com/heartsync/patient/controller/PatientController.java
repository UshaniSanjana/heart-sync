package com.heartsync.patient.controller;

import com.heartsync.patient.dto.PatientRequest;
import com.heartsync.patient.dto.PatientResponse;
import com.heartsync.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * POST /api/patients
     * X-User-Id header injected by the gateway — no JWT handling here.
     */
    @PostMapping
    public ResponseEntity<PatientResponse> create(
            @Valid @RequestBody PatientRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.create(request, userId));
    }

    /** GET /api/patients/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    /** GET /api/patients  or  GET /api/patients?name=John */
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAll(
            @RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(patientService.search(name));
        }
        return ResponseEntity.ok(patientService.getAll());
    }

    /** PUT /api/patients/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> update(
            @PathVariable String id,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    /** DELETE /api/patients/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
