package org.example.user.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.user.contracts.UserRegisterEvent;
import org.example.user.service.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;

    @RabbitListener(queues = "${user.queue.registration}")
    public void handleUserRegisterEvent(@Payload UserRegisterEvent event){
        userService.handleUserRegister(event);
    }
}
