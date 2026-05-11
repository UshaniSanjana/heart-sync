package com.heartsync.ai.consumer;

import com.heartsync.ai.config.RabbitMQConfig;
import com.heartsync.ai.event.EcgAnalyzedEvent;
import com.heartsync.ai.service.AiInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EcgAnalysisConsumer {

    private final AiInferenceService aiInferenceService;

    /**
     * @RabbitListener binds this method to the ECG analyzed queue.
     * Every time ECG service uploads + analyzes an ECG, this fires.
     * Spring AMQP deserializes the JSON message into EcgAnalyzedEvent automatically
     * because we configured Jackson2JsonMessageConverter in RabbitMQConfig.
     */
    @RabbitListener(queues = RabbitMQConfig.ECG_QUEUE)
    public void onEcgAnalyzed(EcgAnalyzedEvent event) {
        log.info("Received EcgAnalyzedEvent: ecgRecordId={}, patientId={}",
                event.getEcgRecordId(), event.getPatientId());
        aiInferenceService.analyze(event);
    }
}
