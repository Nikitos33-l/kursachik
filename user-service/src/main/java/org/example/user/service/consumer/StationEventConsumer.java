package org.example.user.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.service.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final UserService userService;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleDeleteStation(@Payload Long stationId) {
        log.info("Вычитано событие удаления СТО из очереди. ID станции: {}", stationId);

        userService.deleteByWorkplace(stationId);

        log.info("Очистка пользователей для СТО ID: {} успешно завершена по событию из очереди", stationId);
    }
}