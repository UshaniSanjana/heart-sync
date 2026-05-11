package com.heartsync.ai.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document — stored in the "analysis_results" collection.
 * MongoDB's flexible schema is ideal here: AI model output can vary
 * between model versions without requiring a database migration.
 */
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

    // Raw JSON from the model (kept for audit trail)
    private String rawModelOutput;

    @CreatedDate
    private LocalDateTime analyzedAt;
}
