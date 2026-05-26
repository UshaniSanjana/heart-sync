package com.heartsync.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AngiogramAnalysisResponse {

    private String overlayBase64;
    private String maskBase64;
    private double confidence;
    private String overallRisk;
    private int totalBranches;
    private boolean calibrated;
    private List<LesionItem> lesions;
    private int segmentationTimeMs;
    private int qcaTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LesionItem {
        private int rank;
        private double dsPercent;
        private String severity;
        private double mldPx;
        private double rvdPx;
        private double lengthPx;
        private Double mldMm;
        private Double rvdMm;
        private Double lengthMm;
        private List<Integer> narrowestPoint;
    }
}
