package org.example.user.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.user.service.entity.OutboxEvent;
import org.example.user.service.entity.OutboxStatus;
import org.example.user.service.repository.OutboxEventRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventScheduler {
    private final OutboxEventRepository eventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(
            name = "outboxPublisherLock",
            lockAtMostFor = "10s",
            lockAtLeastFor = "300ms"
    )
    public void processOutboxEvents(){
       List<OutboxEvent> events = eventRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(50));

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox обнаружил {} событий для отправки в брокер", events.size());

        for (OutboxEvent e : events) {
            try {
                sendAndUpdateStatus(e);
            } catch (Exception ex) {
                log.error("Критическая ошибка при отправке Outbox события ID: {}. Оно будет пропущено до следующего цикла.", e.getEventId(), ex);

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
        return MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(event.getEventId().toString())
                .build();
    }
}
