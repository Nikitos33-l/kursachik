package com.example.payment.service.scheduler;

import com.example.payment.service.entity.OutboxEvent;
import com.example.payment.service.entity.OutboxStatus;
import com.example.payment.service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {
    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvents(){
        List<OutboxEvent> events = outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(50));

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

    private void sendAndUpdateStatus(OutboxEvent event){
        Message ampqMessage = buildMessage(event);
        rabbitTemplate.send(event.getExchange(),event.getRoutingKey(),ampqMessage);
        log.info("Событие Outbox ID: {} успешно отправлено в RabbitMQ (Exchange: '{}', RoutingKey: '{}')",
                event.getEventId(), event.getExchange(), event.getRoutingKey());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            event.setStatus(OutboxStatus.PROCESSED);
            outboxEventRepository.save(event);
        });
        log.info("Статус события Outbox ID: {} успешно изменен в БД на PROCESSED", event.getEventId());
    }

    private Message buildMessage(OutboxEvent event){
        return MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(event.getEventId().toString())
                .build();
    }
}
