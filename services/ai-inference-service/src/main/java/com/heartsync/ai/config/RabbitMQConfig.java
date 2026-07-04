package com.heartsync.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE        = "heartsync.exchange";
    public static final String AI_ANALYSIS_QUEUE       = "ai.analysis.requested.queue";
    public static final String AI_ANALYSIS_ROUTING_KEY = "ai.analysis.requested";
    public static final String AI_ANALYSIS_DLQ         = "ai.analysis.requested.dlq";
    public static final String DEAD_LETTER_EXCHANGE    = "heartsync.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "ai.analysis.requested.dead";
    public static final String AI_ROUTING_KEY  = "ai.completed";
    public static final String AI_QUEUE        = "ai.completed.queue";

    @Bean
    public TopicExchange heartSyncExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Queue this service CONSUMES from (published by upload services through outbox publishers)
    @Bean
    public Queue aiAnalysisRequestedQueue() {
        return QueueBuilder.durable(AI_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding aiAnalysisRequestedBinding(Queue aiAnalysisRequestedQueue, TopicExchange heartSyncExchange) {
        return BindingBuilder.bind(aiAnalysisRequestedQueue).to(heartSyncExchange).with(AI_ANALYSIS_ROUTING_KEY);
    }

    // Queue this service PUBLISHES to (consumed by Reporting service)
    @Bean
    public Queue aiCompletedQueue() {
        return QueueBuilder.durable(AI_QUEUE).build();
    }

    @Bean
    public Binding aiCompletedBinding(Queue aiCompletedQueue, TopicExchange heartSyncExchange) {
        return BindingBuilder.bind(aiCompletedQueue).to(heartSyncExchange).with(AI_ROUTING_KEY);
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
        return BindingBuilder.bind(aiAnalysisRequestedDlq).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
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

    // Tell @RabbitListener to use JSON deserialization
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAdviceChain(aiAnalysisRetryInterceptor());
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor aiAnalysisRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
