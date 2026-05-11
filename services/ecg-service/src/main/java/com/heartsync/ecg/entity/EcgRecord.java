package com.heartsync.ecg.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ecg_records")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcgRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String patientId;

    private String uploadedBy;      // X-User-Id from gateway header

    private String fileKey;          // MinIO object key  e.g. "ecg/2024/abc123.pdf"
    private String fileName;         // original filename

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    // --- ECG analysis results (populated after mock analysis) ---
    private Integer heartRate;       // bpm
    private String  rhythm;          // e.g. "Normal sinus rhythm"
    private Integer prInterval;      // ms
    private Integer qrsDuration;     // ms
    private Integer qtInterval;      // ms

    @Column(columnDefinition = "TEXT")
    private String findings;         // free-text clinical findings

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime analyzedAt;

    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED
    }
}
