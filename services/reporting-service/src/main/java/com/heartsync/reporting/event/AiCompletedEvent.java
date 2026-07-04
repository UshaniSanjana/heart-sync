package com.heartsync.reporting.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Mirror of ai-inference-service's AiCompletedEvent
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompletedEvent {
    @Builder.Default
    private int version = 1;
    private String requestId;
    private String idempotencyKey;
    private String traceId;
    private String analysisType;
    private String analysisResultId;
    private String angiogramResultId;
    private String ecgRecordId;
    private String patientId;
    private String overallRisk;
}
