package com.heartsync.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AngiogramAnalysisAcceptedResponse {
    private String requestId;
    private String idempotencyKey;
    private String traceId;
    private String status;
    private String message;
}
