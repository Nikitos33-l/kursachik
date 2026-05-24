package org.example.station.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    private final String stationExchange;

    public RabbitConfig(@Value("${station.exchange.name}") String stationExchange) {
        this.stationExchange = stationExchange;
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(stationExchange,true,false);
    }

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
