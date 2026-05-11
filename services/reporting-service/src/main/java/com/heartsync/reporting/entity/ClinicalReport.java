package com.heartsync.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinical_reports")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String patientId;

    private String ecgRecordId;
    private String aiAnalysisId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATING;

    private String overallRisk;         // LOW / MODERATE / HIGH

    @Column(columnDefinition = "TEXT")
    private String clinicalSummary;

    @Column(columnDefinition = "TEXT")
    private String finalImpression;

    private String pdfKey;              // MinIO object key for the PDF
    private String generatedBy;         // user ID who triggered this

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public enum ReportStatus {
        GENERATING, COMPLETED, FAILED
    }
}
