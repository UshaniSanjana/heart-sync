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
    private String analysisResultId;
    private String ecgRecordId;
    private String patientId;
    private String overallRisk;
}
