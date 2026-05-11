package com.heartsync.patient.service;

import com.heartsync.patient.dto.PatientRequest;
import com.heartsync.patient.dto.PatientResponse;
import com.heartsync.patient.entity.Patient;
import com.heartsync.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientResponse create(PatientRequest request, String registeredBy) {
        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .bloodType(request.getBloodType())
                .medicalHistory(request.getMedicalHistory())
                .referringPhysician(request.getReferringPhysician())
                .consentGiven(request.isConsentGiven())
                .registeredBy(registeredBy)
                .build();

        return PatientResponse.from(patientRepository.save(patient));
    }

    public PatientResponse getById(String id) {
        return patientRepository.findById(id)
                .map(PatientResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
    }

    public List<PatientResponse> getAll() {
        return patientRepository.findAll().stream()
                .map(PatientResponse::from)
                .toList();
    }

    public List<PatientResponse> search(String name) {
        return patientRepository.searchByName(name).stream()
                .map(PatientResponse::from)
                .toList();
    }

    public PatientResponse update(String id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setReferringPhysician(request.getReferringPhysician());
        patient.setConsentGiven(request.isConsentGiven());

        return PatientResponse.from(patientRepository.save(patient));
    }

    public void delete(String id) {
        if (!patientRepository.existsById(id)) {
            throw new IllegalArgumentException("Patient not found: " + id);
        }
        patientRepository.deleteById(id);
    }
}
