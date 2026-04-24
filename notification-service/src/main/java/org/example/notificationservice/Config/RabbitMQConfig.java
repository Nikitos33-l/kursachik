package org.example.notificationservice.Config;


import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private final String queue;
    private final String exchange;
    private final String routingKey;

    public RabbitMQConfig(@Value("${notification.queue}") String queue, @Value("${notification.exchange}") String exchange,@Value("${notification.routing-key}") String routingKey) {
        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Bean
    public Queue createQueue(){
        return new Queue(queue,true);
    }

    @Bean
    public TopicExchange createExchange(){
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding createBinding(Queue queue,TopicExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
