package org.example.security.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    private final String exchangeName;

    private final String createdQueue;
    private final String updatedQueue;
    private final String deletedQueue;

    private final String createdRoutingKey;
    private final String updatedRoutingKey;
    private final String deletedRoutingKey;

    public RabbitMqConfig(
            @Value("${user.exchange.name}") String exchangeName,
            @Value("${security.queue.user-created}") String createdQueue,
            @Value("${security.queue.user-updated}") String updatedQueue,
            @Value("${security.queue.user-deleted}") String deletedQueue,
            @Value("${user.create.routing.key}") String createdRoutingKey,
            @Value("${user.update.routing.key}") String updatedRoutingKey,
            @Value("${user.delete.routing.key}") String deletedRoutingKey) {
        this.exchangeName = exchangeName;
        this.createdQueue = createdQueue;
        this.updatedQueue = updatedQueue;
        this.deletedQueue = deletedQueue;
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
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
