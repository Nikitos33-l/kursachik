package org.example.order.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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

    public RabbitMqConfig(@Value(value = "${notification.exchange}") String exchange, @Value(value = "${user.event.exchange}") String userEventsExchange, @Value(value = "${user.delete.queue}") String userDeleteQueue,@Value(value = "${user.delete.routing.key}") String userDeleteRoutingKey) {
        this.notificationExchange = exchange;
        this.userEventsExchange = userEventsExchange;
        this.userDeleteQueue = userDeleteQueue;
        this.userDeleteRoutingKey = userDeleteRoutingKey;
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
    Queue userDeleteQueue(){
        return new Queue(userDeleteQueue);
    }

    @Bean
    Binding userDeleteBinding(){
        return BindingBuilder.bind(userDeleteQueue()).to(userEventExchange()).with(userDeleteRoutingKey);
    }
    @Bean
    MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }
}
