package org.example.order.service.producer;

import lombok.extern.slf4j.Slf4j;
import org.example.order.service.dto.OrderNotificationDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationRabbitMQProducer {

    private final RabbitTemplate template;
    private final String exchange;
    private final String routingKey;

    public NotificationRabbitMQProducer(
            RabbitTemplate template,
            @Value("${notification.exchange}") String exchange,
            @Value("${notification.routing.key}") String routingKey
    ) {
        this.template = template;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void sendNotification(OrderNotificationDto notificationDto) {
        log.info("[RABBITMQ PRODUCER] Отправка уведомления по заказу ID: {} (Тип: {}) -> Exchange: '{}', RoutingKey: '{}'",
                notificationDto.orderId(), notificationDto.type(), exchange, routingKey);

        template.convertAndSend(exchange, routingKey, notificationDto);

        log.debug("[RABBITMQ PRODUCER] Уведомление для заказа ID: {} успешно передано брокеру", notificationDto.orderId());
    }
}