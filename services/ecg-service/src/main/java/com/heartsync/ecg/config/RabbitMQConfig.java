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

    // This service publishes to this routing key
    public static final String ECG_ROUTING_KEY = "ecg.analyzed";
    public static final String ECG_QUEUE       = "ecg.analyzed.queue";

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
    public Queue ecgAnalyzedQueue() {
        return QueueBuilder.durable(ECG_QUEUE).build();
    }

    @Bean
    public Binding ecgAnalyzedBinding(Queue ecgAnalyzedQueue, TopicExchange heartSyncExchange) {
        return BindingBuilder.bind(ecgAnalyzedQueue)
                .to(heartSyncExchange)
                .with(ECG_ROUTING_KEY);
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
