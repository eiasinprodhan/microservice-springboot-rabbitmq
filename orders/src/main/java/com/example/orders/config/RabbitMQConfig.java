package com.example.orders.config;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE =
            "order.exchange";

    public static final String ORDER_QUEUE =
            "order.queue";

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
    public Queue orderQueue() {
        return new Queue(
                ORDER_QUEUE,
                true
        );
    }

    @Bean
    public Binding orderBinding(
            Queue orderQueue,
            TopicExchange orderExchange
    ) {
        return BindingBuilder
                .bind(orderQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(
            JsonMapper jsonMapper
    ) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter converter
    ) {
        RabbitTemplate template =
                new RabbitTemplate(connectionFactory);

        template.setMessageConverter(converter);

        return template;
    }
}
