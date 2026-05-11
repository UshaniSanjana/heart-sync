package com.heartsync.reporting.consumer;

import com.heartsync.reporting.config.RabbitMQConfig;
import com.heartsync.reporting.event.AiCompletedEvent;
import com.heartsync.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiCompletedConsumer {

    private final ReportingService reportingService;

    /**
     * Fires when AI Inference Service completes coronary analysis.
     * This is the trigger for automatic report generation —
     * the doctor does not need to manually request a report.
     */
    @RabbitListener(queues = RabbitMQConfig.AI_QUEUE)
    public void onAiCompleted(AiCompletedEvent event) {
        log.info("Received AiCompletedEvent: analysisId={}, patientId={}",
                event.getAnalysisResultId(), event.getPatientId());
        reportingService.generateReport(event);
    }
}
