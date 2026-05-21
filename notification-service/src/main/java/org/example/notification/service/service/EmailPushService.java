package org.example.notification.service.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.service.exception.EmailSenderException;
import org.example.notification.service.dto.Request;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailPushService {

    private final JavaMailSender sender;

    public void sendMessage(Request request){
        try {
            var message = buildMessage(request);
            sender.send(message);
        }
        catch (Exception e){
            throw new EmailSenderException("Ошибка отправки email сообщения");
        }
    }

    private SimpleMailMessage buildMessage(Request request){
        SimpleMailMessage message = new SimpleMailMessage();

        var type = request.type();
        String subject = type.getSubject();
        String text = String.format(type.getTemplate(),request.orderId());

        message.setTo(request.email());
        message.setSubject(subject);
        message.setText(text);

        return message;
    }
}
