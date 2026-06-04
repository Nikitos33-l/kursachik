package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.order.service.service.OrderManagementService;
import org.example.order.service.service.UserIntegrationWrapper;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserEventRabbitConsumer {
    private final OrderManagementService orderService;
    private final UserIntegrationWrapper userIntegrationWrapper;

    @RabbitListener(queues = "${user.delete.queue}")
    public void handleUserDelete(@Payload UUID userId){
        orderService.deleteOrderByClient(userId);
        userIntegrationWrapper.evictCache(userId);
    }

    @RabbitListener(queues = "${user.update.queue}")
    public void handleUpdateUser(@Payload UserUpdateEvent event){
        userIntegrationWrapper.evictCache(event.id());
    }

}
