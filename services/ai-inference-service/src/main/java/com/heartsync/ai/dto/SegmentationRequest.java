package com.heartsync.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SegmentationRequest {

    @JsonProperty("image_base64")
    private String imageBase64;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("record_id")
    private String recordId;
}
