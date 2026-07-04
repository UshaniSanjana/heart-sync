package com.heartsync.patient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    @JsonAlias("contactNumber")
    private String phone;
    private String address;
    private String bloodType;
    private String medicalHistory;
    private String referringPhysician;
    private boolean consentGiven;
}
