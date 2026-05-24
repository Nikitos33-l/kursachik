package org.example.user.service.config;

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
public class RabbitConfig {

    private final String exchangeName;
    private final String userRegisterQueue;
    private final String userRegisterRoutingKey;
    private final String registerDlq;
    private final String deadLetterExchange;
    private final String stationExchange;
    private final String stationDeleteQueue;
    private final String stationDeleteRoutingKey;
    private final String stationDeleteDlq;

    public RabbitConfig
            (@Value("${user.exchange.name}") String exchangeName,
             @Value("${user.queue.registration}") String userRegisterQueue,
             @Value("${user.register.routing.key}") String userRegisterRoutingKey,
             @Value("${user.dlq.user-register}") String registerDlq,
             @Value("${dead.letter.exchange.name}") String deadLetterExchange,
             @Value("${station.exchange.name}") String stationExchange,
             @Value("${station.delete.queue}") String stationDeleteQueue,
             @Value("${station.delete.routing.key}") String stationDeleteRoutingKey,
             @Value("${user.dlq.station-delete}") String stationDeleteDlq) {
        this.exchangeName = exchangeName;
        this.userRegisterQueue = userRegisterQueue;
        this.userRegisterRoutingKey = userRegisterRoutingKey;
        this.registerDlq = registerDlq;
        this.deadLetterExchange = deadLetterExchange;
        this.stationExchange = stationExchange;
        this.stationDeleteQueue = stationDeleteQueue;
        this.stationDeleteRoutingKey = stationDeleteRoutingKey;
        this.stationDeleteDlq = stationDeleteDlq;
    }

    @Bean
    public TopicExchange userExchange(){
        return new TopicExchange(exchangeName,true,false);
    }

    @Bean
    public Queue registerEventQueue(){
        return new Queue(userRegisterQueue);
    }

    @Bean
    public Binding userRegisterBinding(){
        return BindingBuilder.bind(registerEventQueue()).to(userExchange()).with(userRegisterRoutingKey);
    }

    @Bean TopicExchange stationExchange(){
        return new TopicExchange(stationExchange);
    }

    @Bean
    public Queue stationDeleteQueue(){
        return new Queue(stationDeleteQueue);
    }

    @Bean
    public Binding stationDeleteBinding(){
        return BindingBuilder.bind(stationDeleteQueue()).to(stationExchange()).with(stationDeleteRoutingKey);
    }


    @Bean
    public TopicExchange deadLetterExchange(){
        return new TopicExchange(deadLetterExchange,true,false);
    }

    @Bean
    public Queue userRegisterDlq(){
        return new Queue(registerDlq);
    }

    @Bean
    public Binding registerDlqBinding(){
        return BindingBuilder.bind(userRegisterDlq()).to(deadLetterExchange()).with(userRegisterRoutingKey);
    }

    @Bean
    public Queue stationDeleteDlq() {
        return new Queue(stationDeleteDlq);
    }

    @Bean
    public Binding stationDeleteDlqBinding() {
        return BindingBuilder.bind(stationDeleteDlq()).to(deadLetterExchange()).with(stationDeleteRoutingKey);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate template){
        return new RepublishMessageRecoverer(template,deadLetterExchange);
    }
}
