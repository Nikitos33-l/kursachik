package org.example.user.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.example.user.service.repository.VehicleRepository;
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
public class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected VehicleRepository vehicleRepository;

    @ServiceConnection
    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @ServiceConnection(name = "redis")
    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @ServiceConnection
    @Container
    protected static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @BeforeEach
    void cleanUp() {
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
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