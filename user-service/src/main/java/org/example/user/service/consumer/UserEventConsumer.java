package org.example.user.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.contracts.UserRegisterEvent;
import org.example.user.service.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {
    private final UserService userService;

    @RabbitListener(queues = "${user.queue.registration}")
    public void handleUserRegisterEvent(@Payload UserRegisterEvent event) {
        log.info("Вычитано событие регистрации из очереди. User ID: {}, Email: {}", event.id(), event.email());
        try {
            userService.handleUserRegister(event);
            log.info("Регистрация пользователя ID: {} успешно обработана асинхронно", event.id());
        } catch (IllegalStateException e) {
            log.warn("Дубликат: пользователь с email {} уже зарегистрирован. Пропускаем сообщение.", event.email());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Дубликат на уровне БД: User ID {} или Email {} уже существует. Пропускаем сообщение.", event.id(), event.email());
        }
    }
}