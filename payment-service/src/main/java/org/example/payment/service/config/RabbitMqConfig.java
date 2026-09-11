package org.example.payment.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    private final String paymentEventExchange;
;

    public RabbitMqConfig(@Value("payment.exchange") String orderEventExchange)
    {
        this.paymentEventExchange = orderEventExchange;
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(paymentEventExchange);
    }

}
