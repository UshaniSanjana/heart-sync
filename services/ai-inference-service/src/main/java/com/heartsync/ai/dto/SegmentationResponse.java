package com.heartsync.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SegmentationResponse {

    @JsonProperty("mask_base64")
    private String maskBase64;

    private double confidence;

    @JsonProperty("processing_time_ms")
    private int processingTimeMs;
}
