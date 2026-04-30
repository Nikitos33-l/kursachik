package org.example.kursach.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private final String exchange;

    public RabbitMQConfig(@Value("${notification.exchange}") String exchange) {
        this.exchange = exchange;
    }

    @Bean
    public TopicExchange createExchange(){
        return new TopicExchange(exchange,true,false);
    }

    @Bean
    public MessageConverter createConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
