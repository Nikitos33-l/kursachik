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

    public RabbitConfig(@Value("${user.exchange.name}") String exchangeName, @Value("${user.queue.registration}") String userRegisterQueue, @Value("${user.register.routing.key}") String userRegisterRoutingKey,@Value("${user.dlq.user-register}") String registerDlq,@Value("${dead.letter.exchange.name}") String deadLetterExchange) {
        this.exchangeName = exchangeName;
        this.userRegisterQueue = userRegisterQueue;
        this.userRegisterRoutingKey = userRegisterRoutingKey;
        this.registerDlq = registerDlq;
        this.deadLetterExchange = deadLetterExchange;
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
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate template){
        return new RepublishMessageRecoverer(template,deadLetterExchange);
    }
}
