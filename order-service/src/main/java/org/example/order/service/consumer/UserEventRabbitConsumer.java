package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.order.service.service.OrderManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.UUID;

@RequiredArgsConstructor
public class UserEventRabbitConsumer {
    private final OrderManagementService orderService;

    @RabbitListener(queues = "${user.delete.queue}")
    public void listenUserDeleteEvent(@Payload UUID userId){
        orderService.deleteOrderByClient(userId);
    }
}
