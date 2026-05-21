package org.example.user.service.publisher;

import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String deleteRoutingKey;
    private final String addRoutingKey;
    private final String updateRoutingKey;

    public UserEventPublisher(RabbitTemplate rabbitTemplate, @Value("${user.exchange.name}") String exchange, @Value("${user.delete.routing.key}") String deleteRoutingKey, @Value("${user.create.routing.key}") String addRoutingKey,@Value("${user.update.routing.key}") String updateRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.deleteRoutingKey = deleteRoutingKey;
        this.addRoutingKey = addRoutingKey;
        this.updateRoutingKey = updateRoutingKey;
    }

    public void publishUserDeletedEvents(UUID userId){
        rabbitTemplate.convertAndSend(exchange,deleteRoutingKey,userId);
    }

    public void publishUserCreatedEvents(UserCreatedEvent message){
        rabbitTemplate.convertAndSend(exchange,addRoutingKey,message);
    }

    public void publishUserUpdateEvents(UserUpdateEvent message){
        rabbitTemplate.convertAndSend(exchange,updateRoutingKey,message);
    }

}
