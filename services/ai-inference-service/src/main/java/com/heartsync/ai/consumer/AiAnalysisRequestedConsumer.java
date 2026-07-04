package com.heartsync.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heartsync.ai.config.RabbitMQConfig;
import com.heartsync.ai.event.AiAnalysisRequestedEvent;
import com.heartsync.ai.service.AiInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisRequestedConsumer {

    private final AiInferenceService aiInferenceService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.AI_ANALYSIS_QUEUE)
    public void onAiAnalysisRequested(Message message) throws Exception {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        AiAnalysisRequestedEvent event = objectMapper.readValue(payload, AiAnalysisRequestedEvent.class);
        log.info("Received AiAnalysisRequestedEvent requestId={} idempotencyKey={} type={} traceId={}",
                event.getRequestId(), event.getIdempotencyKey(), event.getAnalysisType(), event.getTraceId());
        aiInferenceService.orchestrate(event);
    }
}
