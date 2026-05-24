package org.example.notification.service.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notification.service.service.EmailPushService;
import org.example.notification.service.dto.Request;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    EmailPushService service;

    @RabbitListener(queues = "${notification.queue}")
    public void sendMessage(@Valid @Payload Request request){
        service.sendMessage(request);
    }
}
