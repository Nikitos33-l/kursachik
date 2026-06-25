package org.example.notification.service.service;

import org.example.notification.service.dto.OrderNotificationType;
import org.example.notification.service.dto.Request;
import org.example.notification.service.exception.EmailSenderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailPushServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EmailPushService emailPushService;

    @Test
    @DisplayName("Успешная сборка и отправка email с правильными данными, если сообщение уникально")
    void sendMessage_Success() {
        OrderNotificationType mockType = OrderNotificationType.NEW;
        Request request = new Request("client@mail.com", 123L, mockType);
        String messageId = UUID.randomUUID().toString();
        String expectedKey = "notification:idempotency:" + messageId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(expectedKey, "PROCESSED", 24, TimeUnit.HOURS)).thenReturn(true);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailPushService.sendMessage(request, messageId);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getTo()).containsExactly("client@mail.com");
        assertThat(capturedMessage.getSubject()).isEqualTo("Заказ принят");
    }

    @Test
    @DisplayName("В случае падения JavaMailSender должно выбрасываться кастомное EmailSenderException")
    void sendMessage_ShouldThrowEmailSenderException_WhenSenderFails() {
        Request request = new Request("client@mail.com", 123L, OrderNotificationType.NEW);
        String messageId = UUID.randomUUID().toString();
        String expectedKey = "notification:idempotency:" + messageId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(expectedKey, "PROCESSED", 24, TimeUnit.HOURS)).thenReturn(true);

        doThrow(new RuntimeException("SMTP server connection timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailPushService.sendMessage(request, messageId))
                .isInstanceOf(EmailSenderException.class)
                .hasMessageContaining("Ошибка отправки email сообщения");
    }

    @Test
    @DisplayName("Если в Redis уже есть такой ключ, отправка email не происходит (дубликат)")
    void sendMessage_ShouldSkip_WhenMessageIsDuplicate() {
        Request request = new Request("client@mail.com", 123L, OrderNotificationType.NEW);
        String messageId = UUID.randomUUID().toString();
        String expectedKey = "notification:idempotency:" + messageId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(expectedKey, "PROCESSED", 24, TimeUnit.HOURS)).thenReturn(false);

        emailPushService.sendMessage(request, messageId);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}