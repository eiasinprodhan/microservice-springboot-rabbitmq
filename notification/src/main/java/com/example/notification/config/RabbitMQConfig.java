package com.example.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    // ================= EXCHANGES =================
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_BROADCAST_EXCHANGE = "order.broadcast.exchange";
    public static final String ORDER_CALLBACK_EXCHANGE = "order.callback.exchange";
    public static final String ORDER_DLX = "order.dlx";

    // ================= QUEUES =================
    public static final String ORDER_CREATED_QUEUE = "notification.order.created.queue";
    public static final String ORDER_UPDATED_QUEUE = "notification.order.updated.queue";
    public static final String ORDER_CANCELLED_QUEUE = "notification.order.cancelled.queue";
    public static final String ORDER_AUDIT_QUEUE = "notification.order.audit.queue";
    public static final String ORDER_DLQ = "notification.order.dlq";
    public static final String BROADCAST_EMAIL_QUEUE = "notification.broadcast.email.queue";
    public static final String BROADCAST_SMS_QUEUE = "notification.broadcast.sms.queue";
    public static final String ORDER_CALLBACK_QUEUE = "order.notification.callback.queue";

    // ================= ROUTING KEYS =================
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_WILDCARD_ROUTING_KEY = "order.*";
    public static final String ORDER_DLQ_ROUTING_KEY = "order.dead.letter";
    public static final String NOTIFICATION_PROCESSED_ROUTING_KEY = "notification.processed";

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder
                .topicExchange(ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public FanoutExchange orderBroadcastExchange() {
        return ExchangeBuilder
                .fanoutExchange(ORDER_BROADCAST_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange orderCallbackExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_CALLBACK_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(ORDER_DLX)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
                .durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder
                .durable(ORDER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder
                .durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderAuditQueue() {
        return QueueBuilder
                .durable(ORDER_AUDIT_QUEUE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(ORDER_DLQ)
                .build();
    }

    @Bean
    public Queue broadcastEmailQueue() {
        return QueueBuilder
                .durable(BROADCAST_EMAIL_QUEUE)
                .build();
    }

    @Bean
    public Queue broadcastSmsQueue() {
        return QueueBuilder
                .durable(BROADCAST_SMS_QUEUE)
                .build();
    }

    @Bean
    public Queue orderCallbackQueue() {
        return QueueBuilder
                .durable(ORDER_CALLBACK_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderUpdatedBinding(Queue orderUpdatedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderUpdatedQueue)
                .to(orderExchange)
                .with(ORDER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderCancelledQueue)
                .to(orderExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding orderAuditWildcardBinding(Queue orderAuditQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderAuditQueue)
                .to(orderExchange)
                .with(ORDER_WILDCARD_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(ORDER_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding broadcastEmailBinding(Queue broadcastEmailQueue, FanoutExchange orderBroadcastExchange) {
        return BindingBuilder
                .bind(broadcastEmailQueue)
                .to(orderBroadcastExchange);
    }

    @Bean
    public Binding broadcastSmsBinding(Queue broadcastSmsQueue, FanoutExchange orderBroadcastExchange) {
        return BindingBuilder
                .bind(broadcastSmsQueue)
                .to(orderBroadcastExchange);
    }

    @Bean
    public Binding orderCallbackBinding(Queue orderCallbackQueue, DirectExchange orderCallbackExchange) {
        return BindingBuilder
                .bind(orderCallbackQueue)
                .to(orderCallbackExchange)
                .with(NOTIFICATION_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter converter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
