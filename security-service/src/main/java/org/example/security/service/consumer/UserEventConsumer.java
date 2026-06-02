package org.example.security.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.security.service.service.SecurityUserService;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserEventConsumer {

    private final SecurityUserService userService;

    @RabbitListener(queues = "${security.queue.user-created}")
    public void handleUserCreated(@Payload UserCreatedEvent event){
        userService.createUser(event);
    }

    @RabbitListener(queues = "${security.queue.user-updated}")
    public void handleUserUpdated(@Payload UserUpdateEvent event){
        userService.updateUser(event);
    }

    @RabbitListener(queues = "${security.queue.user-deleted}")
    public void handleUserDeleted(@Payload UUID userId){
        userService.deleteUser(userId);
    }
}
