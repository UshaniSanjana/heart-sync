package com.heartsync.ai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published after AI analysis completes.
 * Consumed by the Reporting Service to trigger report generation.
 */
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
