package org.example.payment.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    private final String orderEventExchange;
;

    public RabbitMqConfig(@Value("order.exchange") String orderEventExchange)
    {
        this.orderEventExchange = orderEventExchange;
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(orderEventExchange);
    }

}
