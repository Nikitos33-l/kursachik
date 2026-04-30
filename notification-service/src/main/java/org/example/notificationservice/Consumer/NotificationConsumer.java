package org.example.notificationservice.Consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationservice.Service.EmailPushService;
import org.example.notificationservice.dto.Request;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;

@RequiredArgsConstructor
public class NotificationConsumer {
    EmailPushService service;

    @RabbitListener(queues = "${notification.queue}")
    public void sendMessage(@Valid @Payload Request request){
        service.sendMessage(request);
    }
}
