package org.example.order.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    private final String exchange;

    public RabbitMqConfig(@Value(value = "${notification.exchange}") String exchange) {
        this.exchange = exchange;
    }

    @Bean
    TopicExchange topicExchange(){
        return new TopicExchange(exchange,true,false);
    }
}
