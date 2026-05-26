package com.heartsync.ecg.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published to RabbitMQ after ECG analysis completes.
 * The AI Inference Service consumes this event to trigger
 * coronary segmentation analysis.
 *
 * This class is duplicated in ai-inference-service and reporting-service
 * (same package + fields) so each service can deserialize it.
 * In a production system you'd publish this in a shared library.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcgAnalyzedEvent {
    private String ecgRecordId;
    private String patientId;
    private String fileKey;          // MinIO object key for ECG image download
    private Integer heartRate;
    private String rhythm;
    private Integer prInterval;
    private Integer qrsDuration;
    private Integer qtInterval;
    private String findings;
    private String angiogramImageKey;
}
