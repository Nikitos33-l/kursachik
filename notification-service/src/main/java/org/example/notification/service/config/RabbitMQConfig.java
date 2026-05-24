package org.example.notification.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private final String queue;
    private final String exchange;
    private final String routingKey;

    private final String deadLetterExchange;
    private final String dlq;

    public RabbitMQConfig(
            @Value("${notification.queue}") String queue,
            @Value("${notification.exchange}") String exchange,
            @Value("${notification.routing.key}") String routingKey,
            @Value("${dead.letter.exchange.name}") String deadLetterExchange,
            @Value("${notification.dlq}") String dlq) {

        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.deadLetterExchange = deadLetterExchange;
        this.dlq = dlq;
    }

    @Bean
    public TopicExchange notificationExchange(){
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue notificationQueue(){
        return new Queue(queue);
    }

    @Bean
    public Binding notificationBinding(){
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(routingKey);
    }

    @Bean
    public TopicExchange deadLetterExchange(){
        return new TopicExchange(deadLetterExchange, true, false);
    }

    @Bean
    public Queue notificationDlq(){
        return new Queue(dlq);
    }

    @Bean
    public Binding notificationDlqBinding(){
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with(routingKey);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate template){
        return new RepublishMessageRecoverer(template, deadLetterExchange);
    }
}