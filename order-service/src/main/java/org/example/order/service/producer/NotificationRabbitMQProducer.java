package org.example.order.service.producer;

import org.example.order.service.dto.OrderNotificationDto;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationRabbitMQProducer {

    private final RabbitTemplate template;
    private final String exchange;
    private final String routingKey;

    public NotificationRabbitMQProducer(RabbitTemplate template, @Value("${notification.exchange}") String exchange, @Value("${notification.routing.key}") String routingKey) {
        this.template = template;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void sendNotification(OrderNotificationDto notificationDto){
        template.convertAndSend(exchange,routingKey,notificationDto);
    }

}
