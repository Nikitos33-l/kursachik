package org.example.order.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected OrderRepository orderRepository;
    @Autowired protected OrderStatusRepository orderStatusRepository;

    @ServiceConnection
    @Container
    protected static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:18.4-alpine");

    @ServiceConnection
    @Container
    protected static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @ServiceConnection(name = "redis")
    @Container
    protected static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @BeforeEach
    public void setUp(){
        orderRepository.deleteAll();
        orderStatusRepository.deleteAll();
    }

    protected HttpHeaders getSecurityHeaders(String role, Long stationId, UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", userId != null ? userId.toString() : "");
        headers.add("X-Station-Id", stationId != null ? String.valueOf(stationId) : "");
        headers.add("X-User-Roles", role);
        headers.add("X-User-Email", "test@mail.com");
        return headers;
    }
}
