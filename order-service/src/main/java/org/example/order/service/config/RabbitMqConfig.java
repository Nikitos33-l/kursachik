package org.example.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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
public class RabbitMqConfig {
    private final String notificationExchange;
    private final String userEventsExchange;
    private final String userDeleteQueue;
    private final String userDeleteRoutingKey;

    private final String userUpdateQueue;
    private final String userUpdateRoutingKey;
    private final String userUpdateDlq;

    private final String stationEventExchange;
    private final String stationDeleteQueue;
    private final String stationDeleteRoutingKey;

    private final String deadLetterExchange;
    private final String userDeleteDlq;
    private final String stationDeleteDlq;
    private final String stationServicesUpdatedQueue;
    private final String stationServicesUpdatedRoutingKey;
    private final String stationServicesUpdatedDlq;

    public RabbitMqConfig
            (@Value("${notification.exchange}") String exchange,
             @Value("${user.event.exchange}") String userEventsExchange,
             @Value("${user.delete.queue}") String userDeleteQueue,
             @Value("${user.delete.routing.key}") String userDeleteRoutingKey,

             @Value("${user.update.queue}") String userUpdateQueue,
             @Value("${user.update.routing.key}") String userUpdateRoutingKey,
             @Value("${order.dlq.user-update}") String userUpdateDlq,

             @Value("${station.exchange.name}") String stationEventExchange,
             @Value("${station.delete.queue}") String stationDeleteQueue,
             @Value("${station.delete.routing.key}") String stationDeleteRoutingKey,
             @Value("${dead.letter.exchange.name}") String deadLetterExchange,
             @Value("${order.dlq.user-delete}") String userDeleteDlq,
             @Value("${order.dlq.station-delete}") String stationDeleteDlq,
             @Value("${station.services.updated.queue}") String stationServicesUpdatedQueue,
             @Value("${station.services.updated.routing.key}") String stationServicesUpdatedRoutingKey,
             @Value("${order.dlq.station-services-updated}") String stationServicesUpdatedDlq) {

        this.notificationExchange = exchange;
        this.userEventsExchange = userEventsExchange;
        this.userDeleteQueue = userDeleteQueue;
        this.userDeleteRoutingKey = userDeleteRoutingKey;

        this.userUpdateQueue = userUpdateQueue;
        this.userUpdateRoutingKey = userUpdateRoutingKey;
        this.userUpdateDlq = userUpdateDlq;

        this.stationEventExchange = stationEventExchange;
        this.stationDeleteQueue = stationDeleteQueue;
        this.stationDeleteRoutingKey = stationDeleteRoutingKey;
        this.deadLetterExchange = deadLetterExchange;
        this.userDeleteDlq = userDeleteDlq;
        this.stationDeleteDlq = stationDeleteDlq;
        this.stationServicesUpdatedQueue = stationServicesUpdatedQueue;
        this.stationServicesUpdatedRoutingKey = stationServicesUpdatedRoutingKey;
        this.stationServicesUpdatedDlq = stationServicesUpdatedDlq;
    }

    @Bean
    TopicExchange notificationExchange(){
        return new TopicExchange(notificationExchange,true,false);
    }

    @Bean
    TopicExchange userEventExchange(){
        return new TopicExchange(userEventsExchange,true,false);
    }

    @Bean
    TopicExchange stationEventExchange(){
        return new TopicExchange(stationEventExchange,true,false);
    }

    @Bean
    TopicExchange deadLetterExchange(){
        return new TopicExchange(deadLetterExchange, true, false);
    }

    @Bean
    Queue stationDeleteQueue(){
        return new Queue(stationDeleteQueue);
    }

    @Bean
    Queue userDeleteQueue(){
        return new Queue(userDeleteQueue);
    }

    @Bean
    Queue userDeleteDlq(){
        return new Queue(userDeleteDlq);
    }

    @Bean
    Queue userUpdateQueue(){
        return new Queue(userUpdateQueue);
    }

    @Bean
    Queue userUpdateDlq(){
        return new Queue(userUpdateDlq);
    }

    @Bean
    Queue stationDeleteDlq(){
        return new Queue(stationDeleteDlq);
    }

    @Bean
    Binding userDeleteBinding(){
        return BindingBuilder.bind(userDeleteQueue()).to(userEventExchange()).with(userDeleteRoutingKey);
    }

    @Bean
    Binding userUpdateBinding(){
        return BindingBuilder.bind(userUpdateQueue()).to(userEventExchange()).with(userUpdateRoutingKey);
    }

    @Bean
    Binding stationDeleteBinding(){
        return BindingBuilder.bind(stationDeleteQueue()).to(stationEventExchange()).with(stationDeleteRoutingKey);
    }

    @Bean
    Binding userDeleteDlqBinding(){
        return BindingBuilder.bind(userDeleteDlq()).to(deadLetterExchange()).with(userDeleteRoutingKey);
    }

    @Bean
    Binding userUpdateDlqBinding(){
        return BindingBuilder.bind(userUpdateDlq()).to(deadLetterExchange()).with(userUpdateRoutingKey);
    }

    @Bean
    Binding stationDeleteDlqBinding(){
        return BindingBuilder.bind(stationDeleteDlq()).to(deadLetterExchange()).with(stationDeleteRoutingKey);
    }

    @Bean
    Queue stationServicesUpdatedQueue(){
        return new Queue(stationServicesUpdatedQueue);
    }

    @Bean
    Queue stationServicesUpdatedDlq(){
        return new Queue(stationServicesUpdatedDlq);
    }

    @Bean
    Binding stationServicesUpdatedBinding(){
        return BindingBuilder.bind(stationServicesUpdatedQueue())
                .to(stationEventExchange())
                .with(stationServicesUpdatedRoutingKey);
    }

    @Bean
    Binding stationServicesUpdatedDlqBinding(){
        return BindingBuilder.bind(stationServicesUpdatedDlq())
                .to(deadLetterExchange())
                .with(stationServicesUpdatedRoutingKey);
    }

    @Bean
    MessageConverter converter(ObjectMapper objectMapper){
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate template){
        return new RepublishMessageRecoverer(template, deadLetterExchange);
    }
}