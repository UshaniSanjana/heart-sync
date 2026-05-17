package com.heartsync.ai.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "analysis_results")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @Id
    private String id;

    @Indexed
    private String ecgRecordId;

    @Indexed
    private String patientId;

    @Builder.Default
    private String status = "COMPLETED";

    // Coronary artery findings: artery name → status (Normal / Stenosis / Occlusion)
    private Map<String, String> coronaryFindings;

    private Integer estimatedStenosisPercent;   // 0–100

    private String calciumScore;                // e.g. "Within normal range"
    private String dominance;                   // "Right" or "Left"

    private String overallRisk;                 // LOW / MODERATE / HIGH

    // Per-lesion QCA output from MobileUNetv3 + QCA pipeline
    private List<LesionResult> lesions;
    private Integer totalBranches;
    private Boolean calibrated;

    // MinIO object keys for derived images
    private String angiogramImageKey;
    private String segmentationMaskKey;
    private String overlayImageKey;

    // Raw JSON from the model (kept for audit trail)
    private String rawModelOutput;

    @CreatedDate
    private LocalDateTime analyzedAt;

    @Getter @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LesionResult {
        private Integer rank;
        private Double dsPercent;
        private String severity;
        private Double mldPx;
        private Double rvdPx;
        private Double lengthPx;
        private Double mldMm;
        private Double rvdMm;
        private Double lengthMm;
        private List<Integer> narrowestPoint;
    }
}
