package org.example.order.service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.api.client.UserServiceFeignClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.example.order.service.constant.CacheNames.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIntegrationWrapper {

    private final UserServiceFeignClient userServiceFeignClient;

    @Cacheable(value = USER_EMAIL_CACHE, key = "#clientId")
    @CircuitBreaker(name = "userServiceBreaker")
    public String getEmailByUserId(UUID clientId) {
        log.info("[CACHE MISS] Запрос email для пользователя UUID: {} через User Service REST API", clientId);
        return userServiceFeignClient.getEmailByUserId(clientId);
    }

    @CacheEvict(value = USER_EMAIL_CACHE, key = "#id")
    public void evictCache(UUID id) {
        log.info("[CACHE EVICT] Удален email пользователя UUID: {} из кэша [{}]", id, USER_EMAIL_CACHE);
    }
}