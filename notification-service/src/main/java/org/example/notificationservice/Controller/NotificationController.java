package org.example.notificationservice.Controller;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.Service.EmailPushService;
import org.example.notificationservice.dto.Request;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@RequiredArgsConstructor
public class NotificationController {
    EmailPushService service;

    @RabbitListener(queues = "${notification.queue}")
    public void sendMessage(Request request){
        service.sendMessage(request);
    }
}
