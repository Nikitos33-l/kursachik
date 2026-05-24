package org.example.user.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.user.service.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final UserService userService;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleDeleteStation(@Payload Long stationId){
        userService.deleteByWorkplace(stationId);
    }
}
