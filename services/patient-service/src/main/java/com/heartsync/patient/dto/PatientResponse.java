package com.heartsync.patient.dto;

import com.heartsync.patient.entity.Patient;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PatientResponse {

    private String id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String phone;
    private String address;
    private String bloodType;
    private String medicalHistory;
    private String referringPhysician;
    private boolean consentGiven;
    private String registeredBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PatientResponse from(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .email(p.getEmail())
                .phone(p.getPhone())
                .address(p.getAddress())
                .bloodType(p.getBloodType())
                .medicalHistory(p.getMedicalHistory())
                .referringPhysician(p.getReferringPhysician())
                .consentGiven(p.isConsentGiven())
                .registeredBy(p.getRegisteredBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
