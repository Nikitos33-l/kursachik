package org.example.user.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private String exchangeName;

    public RabbitConfig(@Value("${user.exchange.name}") String exchangeName) {
        this.exchangeName = exchangeName;
    }

    @Bean
    public TopicExchange userExchange(){
        return new TopicExchange(exchangeName,true,false);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }
}
