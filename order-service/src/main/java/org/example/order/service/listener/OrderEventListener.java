package org.example.order.service.listener;

import lombok.RequiredArgsConstructor;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.example.order.service.mapper.OrderEventMapper;
import org.example.order.service.producer.NotificationRabbitMQProducer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final NotificationRabbitMQProducer producer;
    private final OrderEventMapper mapper;

    @TransactionalEventListener
    public void handleStatusChange(OrderStatusChangeEvent event){
        producer.sendNotification(mapper.toDto(event));
    }

    @TransactionalEventListener
    public void handleWorkerAssignment(WorkerAssignmentEvent event){
        producer.sendNotification(mapper.toDto(event));
    }
}
