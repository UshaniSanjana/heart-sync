package com.heartsync.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class QcaResponse {

    private List<QcaLesionResult> lesions;

    @JsonProperty("total_branches")
    private int totalBranches;

    @JsonProperty("overall_risk")
    private String overallRisk;

    private boolean calibrated;

    @JsonProperty("overlay_base64")
    private String overlayBase64;

    @JsonProperty("processing_time_ms")
    private int processingTimeMs;
}
