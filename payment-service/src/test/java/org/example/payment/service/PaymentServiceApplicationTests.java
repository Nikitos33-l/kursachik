package org.example.payment.service;

import org.example.order.service.api.common.OrderServiceFeignClient;
import org.example.order.service.api.common.dto.OrderTotalResponse;
import org.example.payment.service.dto.RemotePaymentResult;
import org.example.payment.service.entity.Payment;
import org.example.payment.service.entity.PaymentStatus;
import org.example.payment.service.repository.OutboxEventRepository;
import org.example.payment.service.repository.PaymentRepository;
import org.example.payment.service.scheduler.OutboxEventScheduler;
import org.example.payment.service.service.ExternalPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class PaymentServiceApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxEventScheduler outboxEventScheduler;

    @MockitoSpyBean private RabbitTemplate rabbitTemplate;

    @MockitoBean private ExternalPaymentService externalPaymentService;
    @MockitoBean private OrderServiceFeignClient orderServiceFeignClient;

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    protected static final RabbitMQContainer rabbitMq =
            new RabbitMQContainer("rabbitmq:3-management-alpine")
                    .withExposedPorts(5672, 15672)
                    .waitingFor(Wait.forHttp("/").forPort(15672).forStatusCode(200));

    private final String testUserId = UUID.randomUUID().toString();
    private final String testUserRoles = "USER";
    private final String testUserEmail = "test@example.com";

    @BeforeEach
    void setUpConfiguration() {
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    private Payment savePayment(Long orderId, BigDecimal amount, PaymentStatus status, String externalId, String url) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentStatus(status)
                .externalId(externalId)
                .confirmationUrl(url)
                .build();
        return paymentRepository.save(payment);
    }

    @Test
    void shouldSuccessfullyInitiateNewPaymentSession() throws Exception {
        Long orderId = 123L;
        BigDecimal expectedTotal = BigDecimal.valueOf(150.00);
        String mockExternalId = "yookassa-external-id-001";
        String mockCheckoutUrl = "https://yookassa.ru/checkout/link";

        Mockito.when(orderServiceFeignClient.getTotalByOrderId(orderId))
                .thenReturn(new OrderTotalResponse(expectedTotal));

        Mockito.when(externalPaymentService.initiatePay(any()))
                .thenReturn(new RemotePaymentResult(mockExternalId, mockCheckoutUrl));

        mockMvc.perform(post("/api/payment/{orderId}", orderId)
                        .header("X-User-Id", testUserId)
                        .header("X-User-Roles", testUserRoles)
                        .header("X-User-Email", testUserEmail)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value(mockCheckoutUrl));

        Optional<Payment> savedPaymentOpt = paymentRepository.findByExternalId(mockExternalId);
        assertTrue(savedPaymentOpt.isPresent());
        Payment savedPayment = savedPaymentOpt.get();
        assertEquals(orderId, savedPayment.getOrderId());
        assertEquals(0, expectedTotal.compareTo(savedPayment.getAmount()));
    }

    @Test
    void shouldReturnExistingActivePaymentSessionToPreventDuplication() throws Exception {
        Long orderId = 456L;
        String existingUrl = "https://yookassa.ru/checkout/existing-link";

        savePayment(orderId, BigDecimal.valueOf(300.00), PaymentStatus.PENDING, "ext-id-456", existingUrl);

        mockMvc.perform(post("/api/payment/{orderId}", orderId)
                        .header("X-User-Id", testUserId)
                        .header("X-User-Roles", testUserRoles)
                        .header("X-User-Email", testUserEmail)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value(existingUrl));

        Mockito.verifyNoInteractions(orderServiceFeignClient);
        Mockito.verifyNoInteractions(externalPaymentService);
    }

    @Test
    void shouldProcessSucceededWebhookSaveOutboxAndPublishToRabbitMQ() throws Exception {
        Long orderId = 789L;
        String externalId = "yookassa-successful-uuid";

        savePayment(orderId, BigDecimal.valueOf(500.00), PaymentStatus.PENDING, externalId, null);

        String webhookJsonPayload = """
                {
                    "event": "payment.succeeded",
                    "type": "notification",
                    "object": {
                        "id": "%s",
                        "status": "succeeded",
                        "amount": { "value": "500.00", "currency": "BYN" }
                    }
                }
                """.formatted(externalId);

        mockMvc.perform(post("/api/payment/webhook/yookassa?secret=webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJsonPayload))
                .andExpect(status().isOk());

        Payment processedPayment = paymentRepository.findByExternalId(externalId).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, processedPayment.getPaymentStatus());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
                    .send(anyString(), anyString(), any(Message.class));
        });
    }
}