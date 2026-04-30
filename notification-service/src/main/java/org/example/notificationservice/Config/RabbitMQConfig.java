package org.example.notificationservice.Config;


import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public DirectExchange createDlxExchange(){
        return new DirectExchange("notification.dlx.exchange",true,false);
    }

    @Bean
    public Queue createDlxQueue(){
        return new Queue("notification.dlx.queue",true);
    }

    @Bean
    public Binding createDlxBinding(){
        return BindingBuilder.bind(createDlxQueue()).to(createDlxExchange()).with("notification.dlx");
    }

    @Bean
    public Queue createQueue(){
        return QueueBuilder.durable(queue).deadLetterExchange("notification.dlx.exchange").deadLetterRoutingKey("notification.dlx").build();
    }

    @Bean
    public TopicExchange createExchange(){
        return new TopicExchange(exchange,true,false);
    }

    @Bean
    public Binding createBinding(@Qualifier(value = "createQueue") Queue queue, TopicExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }


}
