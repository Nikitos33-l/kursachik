package org.example.notification.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.service.exception.EmailSenderException;
import org.example.notification.service.dto.Request;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPushService {

    private final JavaMailSender sender;

    public void sendMessage(Request request) {
        log.info("Инициация отправки email. Тип: [{}], Адресат: '{}', Заказ ID: {}",
                request.type(), request.email(), request.orderId());
        try {
            var message = buildMessage(request);
            sender.send(message);
            log.info("Email успешно отправлен на адрес '{}' по заказу ID: {}", request.email(), request.orderId());
        } catch (Exception e) {
            log.error("Критическая ошибка JavaMailSender при попытке отправить письмо на '{}' (Заказ ID: {}). Причина: {}",
                    request.email(), request.orderId(), e.getMessage(), e);
            throw new EmailSenderException("Ошибка отправки email сообщения");
        }
    }

    private SimpleMailMessage buildMessage(Request request) {
        log.debug("Формирование SimpleMailMessage для заказа ID: {} (Шаблон: {})", request.orderId(), request.type());
        SimpleMailMessage message = new SimpleMailMessage();

        var type = request.type();
        String subject = type.getSubject();
        String text = String.format(type.getTemplate(), request.orderId());

        message.setTo(request.email());
        message.setSubject(subject);
        message.setText(text);

        return message;
    }
}