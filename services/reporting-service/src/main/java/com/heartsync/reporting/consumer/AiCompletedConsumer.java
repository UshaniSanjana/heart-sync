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

    @RabbitListener(queues = RabbitMQConfig.AI_QUEUE)
    public void onAiCompleted(AiCompletedEvent event) {
        log.info("Received AiCompletedEvent requestId={} type={} analysisId={} angiogramResultId={} patientId={} traceId={}",
                event.getRequestId(), event.getAnalysisType(), event.getAnalysisResultId(),
                event.getAngiogramResultId(), event.getPatientId(), event.getTraceId());

        if ("ANGIOGRAM".equals(event.getAnalysisType())) {
            reportingService.generateManual(event.getPatientId(), null);
        } else {
            reportingService.generateReport(event);
        }
    }
}
