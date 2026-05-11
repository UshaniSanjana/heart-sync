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
    private String analysisResultId;
    private String ecgRecordId;
    private String patientId;
    private String overallRisk;
}
