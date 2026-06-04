package org.example.order.service.listener;

import lombok.RequiredArgsConstructor;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.example.order.service.mapper.OrderEventMapper;
import org.example.order.service.producer.NotificationRabbitMQProducer;
import org.example.order.service.service.UserIntegrationWrapper;
import org.example.user.api.client.UserServiceFeignClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final NotificationRabbitMQProducer producer;
    private final OrderEventMapper mapper;
    private final UserIntegrationWrapper userIntegrationWrapper;

    @TransactionalEventListener
    public void handleStatusChange(OrderStatusChangeEvent event){
        String email = (event.userEmail() == null || event.userEmail().isBlank())
                ? userIntegrationWrapper.getEmailByUserId(event.userId())
                : event.userEmail();

        producer.sendNotification(mapper.toDto(event, email));
    }

    @TransactionalEventListener
    public void handleWorkerAssignment(WorkerAssignmentEvent event){
        producer.sendNotification(mapper.toDto(event));
    }
}
