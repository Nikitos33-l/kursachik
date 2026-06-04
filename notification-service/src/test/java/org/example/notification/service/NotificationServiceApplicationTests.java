package org.example.notification.service;

import org.example.notification.service.dto.OrderNotificationType;
import org.example.notification.service.dto.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

    @ServiceConnection
    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${notification.queue}")
    private String notificationQueue;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("Интеграционный тест: успешное чтение сообщения из RabbitMQ и отправка email")
    void shouldConsumeMessageFromRabbitAndSendEmail() {
        String targetEmail = "car-owner@mail.com";
        Long orderId = 555L;
        Request requestPayload = new Request(targetEmail, orderId, OrderNotificationType.IN_PROGRESS);

        rabbitTemplate.convertAndSend(notificationQueue, requestPayload);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    verify(mailSender).send(messageCaptor.capture());

                    SimpleMailMessage sentMessage = messageCaptor.getValue();

                    assertThat(sentMessage.getTo()).containsExactly(targetEmail);
                    assertThat(sentMessage.getSubject()).isEqualTo("Заказ в работе");

                    String expectedText = String.format(OrderNotificationType.IN_PROGRESS.getTemplate(), orderId);
                    assertThat(sentMessage.getText()).isEqualTo(expectedText);
                    assertThat(sentMessage.getText()).contains("555");
                });
    }

    @Test
    @DisplayName("Интеграционный тест: валидация некорректного email в очереди")
    void shouldFailValidationWhenEmailIsInvalid() {
        Request invalidRequest = new Request("invalid-email-format", 123L, OrderNotificationType.NEW);

        rabbitTemplate.convertAndSend(notificationQueue, invalidRequest);

        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    verify(mailSender, org.mockito.Mockito.never()).send(any(SimpleMailMessage.class));
                });
    }
}
