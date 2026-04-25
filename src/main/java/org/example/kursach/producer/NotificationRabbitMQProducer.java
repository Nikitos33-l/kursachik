package org.example.kursach.producer;

import org.example.kursach.dto.OrderNotificationDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationRabbitMQProducer {

    private final RabbitTemplate template;
    private final String exchange;
    private final String routingKey;

    public NotificationRabbitMQProducer(RabbitTemplate template, @Value("${notification.exchange}") String exchange,@Value("${notification.routing-key}") String routingKey) {
        this.template = template;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void sendNotification(OrderNotificationDto notificationDto){
        template.convertAndSend(exchange,routingKey,notificationDto);
    }

}
