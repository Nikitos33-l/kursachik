package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.service.OrderManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final OrderManagementService orderManagementService;

    @RabbitListener(queues = "${order.paid.queue}")
    public void handleOrderPaid(@Payload Long id){
        log.info("[RABBITMQ CONSUMER] Получено событие оплаты заказа . ID: {}", id);
        try {
            RequestOrderStatusDto orderStatusDto = new RequestOrderStatusDto("CLOSED");
            orderManagementService.updateStatus(id,orderStatusDto);
        }
        catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Критический сбой при обработке оплаты заказа ID: {}. Ошибка: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
