package com.heartsync.ai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequestedEvent {
    @Builder.Default
    private int version = 1;
    private String requestId;
    private String idempotencyKey;
    private String traceId;
    private Instant occurredAt;
    private String sourceService;
    private AnalysisType analysisType;
    private String patientId;
    private String ecgRecordId;
    private String fileKey;
    private String angiogramImageKey;
    private Integer heartRate;
    private String rhythm;
    private Integer prInterval;
    private Integer qrsDuration;
    private Integer qtInterval;
    private String findings;

    public enum AnalysisType {
        ECG,
        ANGIOGRAM
    }
}
