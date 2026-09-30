package com.example.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE =
            "order.exchange";

    public static final String ORDER_CREATED_QUEUE =
            "notification.order.created.queue";

    public static final String ORDER_CREATED_ROUTING_KEY =
            "order.created";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(
                ORDER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
                .durable(ORDER_CREATED_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreatedBinding(
            Queue orderCreatedQueue,
            TopicExchange orderExchange
    ) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
