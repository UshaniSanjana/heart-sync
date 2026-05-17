package com.heartsync.ai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Mirror of ecg-service's EcgAnalyzedEvent — must match field names for JSON deserialization
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcgAnalyzedEvent {
    private String ecgRecordId;
    private String patientId;
    private Integer heartRate;
    private String rhythm;
    private Integer prInterval;
    private Integer qrsDuration;
    private Integer qtInterval;
    private String findings;
    private String angiogramImageKey;
}
