package org.example.station.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.entity.OutboxEvent;
import org.example.station.service.entity.OutboxStatus;
import org.example.station.service.repository.OutboxEventRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {
    private final OutboxEventRepository eventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvent(){
        List<OutboxEvent> events = eventRepository.
                findAllByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(50));

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox обнаружил {} событий для отправки в брокер", events.size());

        for(OutboxEvent event : events){
            try {
                sendAndUpdateStatus(event);
            } catch (Exception ex) {
                log.error("Критическая ошибка при отправке Outbox события ID: {}. Оно будет пропущено до следующего цикла.", event.getEventId(), ex);

            }
        }
    }

    private void sendAndUpdateStatus(OutboxEvent event) {
        log.debug("Старт обработки события Outbox ID: {}. Подготовка к отправке в брокер.", event.getEventId());

        Message ampqMessage = buildMessage(event);
        rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), ampqMessage);

        log.debug("Событие Outbox ID: {} успешно отправлено в RabbitMQ (Exchange: '{}', RoutingKey: '{}')",
                event.getEventId(), event.getExchange(), event.getRoutingKey());

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            event.setStatus(OutboxStatus.PROCESSED);
            eventRepository.save(event);
        });

        log.debug("Статус события Outbox ID: {} успешно изменен в БД на PROCESSED", event.getEventId());
    }

    private Message buildMessage(OutboxEvent event){
        return MessageBuilder.withBody(event.getPayload().getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(event.getEventId().toString())
                .build();

    }

}
