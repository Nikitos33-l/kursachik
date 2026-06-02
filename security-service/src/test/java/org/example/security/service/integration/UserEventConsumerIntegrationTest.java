package org.example.security.service.integration;

import org.awaitility.Awaitility;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.service.BlackListService;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class UserEventConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BlackListService blackListService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${user.exchange.name}")
    private String userExchange;

    @Value("${user.create.routing.key}")
    private String userCreatedRoutingKey;

    @Value("${user.update.routing.key}")
    private String userUpdatedRoutingKey;

    @Value("${user.delete.routing.key}")
    private String userDeletedRoutingKey;

    private Role defaultRole;

    @BeforeEach
    void setUp() {
        defaultRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new Role(null, "CLIENT")));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();

        if (redisTemplate.getConnectionFactory() != null) {
            Objects.requireNonNull(redisTemplate.getConnectionFactory())
                    .getConnection()
                    .serverCommands()
                    .flushDb();
        }
    }

    @Test
    @DisplayName("Должен успешно создать пользователя при получении UserCreatedEvent через Exchange")
    void shouldCreateUserWhenUserCreatedEventReceived() {
        UUID newUserId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(
                newUserId,
                "new.user@example.com",
                "encoded_password",
                defaultRole.getName(),
                1L
        );

        rabbitTemplate.convertAndSend(userExchange, userCreatedRoutingKey, event);

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var savedUserOpt = userRepository.findById(newUserId);
                    assertThat(savedUserOpt).isPresent();

                    User savedUser = savedUserOpt.get();
                    assertThat(savedUser.getEmail()).isEqualTo("new.user@example.com");
                });
    }

    @Test
    @DisplayName("Должен обновить email и занести в Blacklist при получении UserUpdateEvent через Exchange")
    void shouldUpdateUserAndBlacklistWhenUserUpdatedEventReceived() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .email("old.email@example.com")
                .password("pass")
                .role(defaultRole)
                .workplaceId(2L)
                .build();
        userRepository.save(existingUser);

        UserUpdateEvent event = new UserUpdateEvent(userId, "new.updated.email@example.com");

        rabbitTemplate.convertAndSend(userExchange, userUpdatedRoutingKey, event);

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    User updatedUser = userRepository.findById(userId).orElseThrow();
                    assertThat(updatedUser.getEmail()).isEqualTo("new.updated.email@example.com");

                    boolean isBlacklisted = blackListService.isUserBlacklisted(userId.toString());
                    assertThat(isBlacklisted).isTrue();
                });
    }

    @Test
    @DisplayName("Должен удалить пользователя из БД и занести в Blacklist при получении ID на удаление")
    void shouldDeleteUserAndBlacklistWhenUserDeletedEventReceived() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .email("to.be.deleted@example.com")
                .password("pass")
                .role(defaultRole)
                .workplaceId(3L)
                .build();
        userRepository.save(existingUser);

        rabbitTemplate.convertAndSend(userExchange, userDeletedRoutingKey, userId);

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    boolean existsInDb = userRepository.findById(userId).isPresent();
                    assertThat(existsInDb).isFalse();

                    boolean isBlacklisted = blackListService.isUserBlacklisted(userId.toString());
                    assertThat(isBlacklisted).isTrue();
                });
    }
}