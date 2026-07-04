package com.heartsync.ai.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "angiogram_results")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AngiogramResult {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private String angiogramImageKey;
    private String overlayBase64;
    private String maskBase64;
    private String  overallRisk;
    private Double  confidence;
    private Integer totalBranches;
    private Boolean calibrated;
    private Integer estimatedStenosisPercent;
    private Integer segmentationTimeMs;
    private Integer qcaTimeMs;
    private List<LesionResult> lesions;

    @CreatedDate
    private LocalDateTime analyzedAt;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LesionResult {
        private int    rank;
        private double dsPercent;
        private String severity;
        private double mldPx;
        private double rvdPx;
        private double lengthPx;
        private Double mldMm;
        private Double rvdMm;
        private Double lengthMm;
    }
}
