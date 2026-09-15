package org.example.station.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    private final String stationExchange;
    private final String orderEventExchange;
    private final String userEventExchange;
    private final String deadLetterExchange;
    private final String deadLetterQueue;

    private final String orderSuccessStationDeleteQueue;
    private final String orderFailedStationDeleteQueue;
    private final String userSuccessStationDeleteQueue;
    private final String userFailedStationDeleteQueue;

    private final String orderSuccessStationDeleteRoutingKey;
    private final String orderFailedStationDeleteRoutingKey;
    private final String userSuccessStationDeleteRoutingKey;
    private final String userFailedStationDeleteRoutingKey;

    public RabbitConfig(@Value("${station.exchange.name}") String stationExchange,
                        @Value("${order.exchange}") String orderEventExchange,
                        @Value("${user.exchange.name}") String userEventExchange,
                        @Value("${dead.letter.exchange.name}") String deadLetterExchange,
                        @Value("${dead.letter.queue.name}") String deadLetterQueue,
                        @Value("${order.success.station.delete.queue}") String orderSuccessStationDeleteQueue,
                        @Value("${order.failed.station.delete.queue}") String orderFailedStationDeleteQueue,
                        @Value("${user.success.station.delete.queue}") String userSuccessStationDeleteQueue,
                        @Value("${user.failed.station.delete.queue}") String userFailedStationDeleteQueue,
                        @Value("${order.success.station.delete.routing.key}") String orderSuccessStationDeleteRoutingKey,
                        @Value("${order.failed.station.delete.routing.key}") String orderFailedStationDeleteRoutingKey,
                        @Value("${user.success.station.delete.routing.key}") String userSuccessStationDeleteRoutingKey,
                        @Value("${user.failed.station.delete.routing.key}") String userFailedStationDeleteRoutingKey) {
        this.stationExchange = stationExchange;
        this.orderEventExchange = orderEventExchange;
        this.userEventExchange = userEventExchange;
        this.deadLetterExchange = deadLetterExchange;
        this.deadLetterQueue = deadLetterQueue;
        this.orderSuccessStationDeleteQueue = orderSuccessStationDeleteQueue;
        this.orderFailedStationDeleteQueue = orderFailedStationDeleteQueue;
        this.userSuccessStationDeleteQueue = userSuccessStationDeleteQueue;
        this.userFailedStationDeleteQueue = userFailedStationDeleteQueue;
        this.orderSuccessStationDeleteRoutingKey = orderSuccessStationDeleteRoutingKey;
        this.orderFailedStationDeleteRoutingKey = orderFailedStationDeleteRoutingKey;
        this.userSuccessStationDeleteRoutingKey = userSuccessStationDeleteRoutingKey;
        this.userFailedStationDeleteRoutingKey = userFailedStationDeleteRoutingKey;
    }


    @Bean
    public TopicExchange stationExchange() {
        return new TopicExchange(stationExchange, true, false);
    }

    @Bean
    public TopicExchange orderEventExchange() {
        return new TopicExchange(orderEventExchange, true, false);
    }

    @Bean
    public TopicExchange userEventExchange() {
        return new TopicExchange(userEventExchange, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(deadLetterExchange, true, false);
    }


    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    @Bean
    public Queue orderSuccessStationDeleteQueue() {
        return QueueBuilder.durable(orderSuccessStationDeleteQueue).build();
    }

    @Bean
    public Queue orderFailedStationDeleteQueue() {
        return QueueBuilder.durable(orderFailedStationDeleteQueue).build();
    }

    @Bean
    public Queue userSuccessStationDeleteQueue() {
        return QueueBuilder.durable(userSuccessStationDeleteQueue).build();
    }

    @Bean
    public Queue userFailedStationDeleteQueue() {
        return QueueBuilder.durable(userFailedStationDeleteQueue).build();
    }


    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("#");
    }

    @Bean
    public Binding orderSuccessStationDeleteBinding(Queue orderSuccessStationDeleteQueue, TopicExchange orderEventExchange) {
        return BindingBuilder.bind(orderSuccessStationDeleteQueue)
                .to(orderEventExchange)
                .with(orderSuccessStationDeleteRoutingKey);
    }

    @Bean
    public Binding orderFailedStationDeleteBinding(Queue orderFailedStationDeleteQueue, TopicExchange orderEventExchange) {
        return BindingBuilder.bind(orderFailedStationDeleteQueue)
                .to(orderEventExchange)
                .with(orderFailedStationDeleteRoutingKey);
    }

    @Bean
    public Binding userSuccessStationDeleteBinding(Queue userSuccessStationDeleteQueue, TopicExchange userEventExchange) {
        return BindingBuilder.bind(userSuccessStationDeleteQueue)
                .to(userEventExchange)
                .with(userSuccessStationDeleteRoutingKey);
    }

    @Bean
    public Binding userFailedStationDeleteBinding(Queue userFailedStationDeleteQueue, TopicExchange userEventExchange) {
        return BindingBuilder.bind(userFailedStationDeleteQueue)
                .to(userEventExchange)
                .with(userFailedStationDeleteRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, deadLetterExchange);
    }
}