package org.example.order.service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    @CircuitBreaker(name = "userServiceBreaker", fallbackMethod = "getEmailByUserIdFallback")
    public String getEmailByUserId(UUID clientId) {
        log.info("[CACHE MISS] Запрос email для пользователя UUID: {} через User Service REST API", clientId);
        return userServiceFeignClient.getEmailByUserId(clientId);
    }


    public String getEmailByUserIdFallback(UUID clientId, Throwable throwable) {
        log.error("[FALLBACK] Ошибка получения email для пользователя {}. Сервис недоступен. Причина: {}",
                clientId, throwable.getMessage());

        return "Данные email временно недоступны";
    }

    @CacheEvict(value = USER_EMAIL_CACHE, key = "#id")
    public void evictCache(UUID id) {
        log.info("[CACHE EVICT] Удален email пользователя UUID: {} из кэша [{}]", id, USER_EMAIL_CACHE);
    }
}