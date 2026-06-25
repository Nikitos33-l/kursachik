package org.example.security.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    private final String exchangeName;
    private final String deadLetterExchangeName;

    private final String createdQueue;
    private final String updatedQueue;
    private final String deletedQueue;

    private final String createdDlq;
    private final String updatedDlq;
    private final String deletedDlq;

    private final String createdRoutingKey;
    private final String updatedRoutingKey;
    private final String deletedRoutingKey;

    public RabbitMqConfig(
            @Value("${user.exchange.name}") String exchangeName,
            @Value("${dead.letter.exchange.name}") String deadLetterExchangeName,
            @Value("${security.queue.user-created}") String createdQueue,
            @Value("${security.queue.user-updated}") String updatedQueue,
            @Value("${security.queue.user-deleted}") String deletedQueue,
            @Value("${security.dlq.user-created}") String createdDlq,
            @Value("${security.dlq.user-updated}") String updatedDlq,
            @Value("${security.dlq.user-deleted}") String deletedDlq,
            @Value("${user.create.routing.key}") String createdRoutingKey,
            @Value("${user.update.routing.key}") String updatedRoutingKey,
            @Value("${user.delete.routing.key}") String deletedRoutingKey) {
        this.exchangeName = exchangeName;
        this.deadLetterExchangeName = deadLetterExchangeName;
        this.createdQueue = createdQueue;
        this.updatedQueue = updatedQueue;
        this.deletedQueue = deletedQueue;
        this.createdDlq = createdDlq;
        this.updatedDlq = updatedDlq;
        this.deletedDlq = deletedDlq;
        this.createdRoutingKey = createdRoutingKey;
        this.updatedRoutingKey = updatedRoutingKey;
        this.deletedRoutingKey = deletedRoutingKey;
    }

    @Bean
    TopicExchange userEventExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue userCreatedQueue() { return new Queue(createdQueue); }

    @Bean
    Queue userUpdatedQueue() { return new Queue(updatedQueue); }

    @Bean
    Queue userDeletedQueue() { return new Queue(deletedQueue); }

    @Bean
    Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue()).to(userEventExchange()).with(createdRoutingKey);
    }

    @Bean
    Binding userUpdatedBinding() {
        return BindingBuilder.bind(userUpdatedQueue()).to(userEventExchange()).with(updatedRoutingKey);
    }

    @Bean
    Binding userDeletedBinding() {
        return BindingBuilder.bind(userDeletedQueue()).to(userEventExchange()).with(deletedRoutingKey);
    }

    @Bean
    TopicExchange deadLetterExchange(){
        return new TopicExchange(deadLetterExchangeName,true,false);
    }

    @Bean
    Queue userCreatedDlq() { return new Queue(createdDlq); }

    @Bean
    Queue userUpdatedDlq() { return new Queue(updatedDlq); }

    @Bean
    Queue userDeletedDlq() { return new Queue(deletedDlq); }

    @Bean
    Binding createdDlqBinding() {
        return BindingBuilder.bind(userCreatedDlq()).to(deadLetterExchange()).with(createdRoutingKey);
    }

    @Bean
    Binding updatedDlqBinding() {
        return BindingBuilder.bind(userUpdatedDlq()).to(deadLetterExchange()).with(updatedRoutingKey);
    }

    @Bean
    Binding deletedDlqBinding() {
        return BindingBuilder.bind(userDeletedDlq()).to(deadLetterExchange()).with(deletedRoutingKey);
    }

    @Bean
    MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate){
        return new RepublishMessageRecoverer(rabbitTemplate,deadLetterExchangeName);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
