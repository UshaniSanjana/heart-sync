package com.heartsync.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class QcaLesionResult {

    private int rank;

    @JsonProperty("ds_percent")
    private double dsPercent;

    private String severity;

    @JsonProperty("mld_px")
    private double mldPx;

    @JsonProperty("rvd_px")
    private double rvdPx;

    @JsonProperty("length_px")
    private double lengthPx;

    @JsonProperty("mld_mm")
    private Double mldMm;

    @JsonProperty("rvd_mm")
    private Double rvdMm;

    @JsonProperty("length_mm")
    private Double lengthMm;

    @JsonProperty("narrowest_point")
    private List<Integer> narrowestPoint;
}
