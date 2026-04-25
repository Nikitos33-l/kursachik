package org.example.kursach.listener;

import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.OrderStatusChangeEvent;
import org.example.kursach.dto.WorkerAssignmentEvent;
import org.example.kursach.mapping.OrderEventMapper;
import org.example.kursach.producer.NotificationRabbitMQProducer;
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
