package com.heartsync.ecg.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange name shared across all services
    public static final String EXCHANGE      = "heartsync.exchange";

    public static final String AI_ANALYSIS_ROUTING_KEY = "ai.analysis.requested";
    public static final String AI_ANALYSIS_QUEUE       = "ai.analysis.requested.queue";
    public static final String AI_ANALYSIS_DLQ         = "ai.analysis.requested.dlq";
    public static final String DEAD_LETTER_EXCHANGE    = "heartsync.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "ai.analysis.requested.dead";

    /**
     * Topic exchange: routes messages by routing key pattern.
     * durable=true means it survives RabbitMQ restarts.
     */
    @Bean
    public TopicExchange heartSyncExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Queue where ECG analyzed events land.
     * The AI service binds to this queue to receive events.
     * Declaring it here too ensures it exists even if AI service hasn't started.
     */
    @Bean
    public Queue aiAnalysisRequestedQueue() {
        return QueueBuilder.durable(AI_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding aiAnalysisRequestedBinding(Queue aiAnalysisRequestedQueue, TopicExchange heartSyncExchange) {
        return BindingBuilder.bind(aiAnalysisRequestedQueue)
                .to(heartSyncExchange)
                .with(AI_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue aiAnalysisRequestedDlq() {
        return QueueBuilder.durable(AI_ANALYSIS_DLQ).build();
    }

    @Bean
    public Binding aiAnalysisRequestedDlqBinding(Queue aiAnalysisRequestedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(aiAnalysisRequestedDlq)
                .to(deadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * Serialize messages as JSON instead of Java binary.
     * JSON is readable in the RabbitMQ management UI and
     * works between services regardless of class location.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
