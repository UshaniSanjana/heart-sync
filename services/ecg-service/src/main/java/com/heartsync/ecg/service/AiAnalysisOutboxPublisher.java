package com.heartsync.ecg.service;

import com.heartsync.ecg.config.RabbitMQConfig;
import com.heartsync.ecg.entity.AiAnalysisOutboxMessage;
import com.heartsync.ecg.repository.AiAnalysisOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisOutboxPublisher {

    private final AiAnalysisOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${outbox.ai-analysis.publish-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        List<AiAnalysisOutboxMessage> messages = outboxRepository.findTop25ByStatusInOrderByCreatedAtAsc(
                List.of(AiAnalysisOutboxMessage.Status.PENDING, AiAnalysisOutboxMessage.Status.FAILED));

        for (AiAnalysisOutboxMessage message : messages) {
            try {
                Message rabbitMessage = MessageBuilder
                        .withBody(message.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("requestId", message.getRequestId())
                        .setHeader("idempotencyKey", message.getIdempotencyKey())
                        .build();
                rabbitTemplate.send(RabbitMQConfig.EXCHANGE, message.getRoutingKey(), rabbitMessage);
                message.setStatus(AiAnalysisOutboxMessage.Status.PUBLISHED);
                message.setPublishedAt(Instant.now());
                message.setLastError(null);
                meterRegistry.counter("heartsync.ai_analysis_requested.published", "source", "ecg-service").increment();
            } catch (Exception e) {
                message.setStatus(AiAnalysisOutboxMessage.Status.FAILED);
                message.setAttempts(message.getAttempts() + 1);
                message.setLastError(e.getMessage());
                meterRegistry.counter("heartsync.ai_analysis_requested.publish_failed", "source", "ecg-service").increment();
                log.warn("Failed to publish AI analysis request {} attempt {}",
                        message.getRequestId(), message.getAttempts(), e);
            }
        }
    }
}
