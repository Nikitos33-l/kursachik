package org.example.security.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;


    @ServiceConnection
    @Container
    protected static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:18.4-alpine");

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

}