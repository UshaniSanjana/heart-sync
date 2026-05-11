package com.heartsync.reporting.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE       = "heartsync.exchange";
    public static final String AI_QUEUE       = "ai.completed.queue";
    public static final String AI_ROUTING_KEY = "ai.completed";

    @Bean
    public TopicExchange heartSyncExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue aiCompletedQueue() {
        return QueueBuilder.durable(AI_QUEUE).build();
    }

    @Bean
    public Binding aiCompletedBinding(Queue aiCompletedQueue, TopicExchange heartSyncExchange) {
        return BindingBuilder.bind(aiCompletedQueue).to(heartSyncExchange).with(AI_ROUTING_KEY);
    }

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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
