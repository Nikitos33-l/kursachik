package org.example.security.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlackListService {

    private final StringRedisTemplate redisTemplate;
    private final String BLACK_LIST_PREFIX = "security-service:blacklist:";

    public void blacklistToken(String jti, long remainingTimeMillis) {
        String key = BLACK_LIST_PREFIX + "tokens:" + jti;
        log.info("[REDIS BLACKLIST] Отзыв токена. JTI: {} помещен в черный список на {} мс", jti, remainingTimeMillis);
        redisTemplate.opsForValue().set(key, "true", Duration.ofMillis(remainingTimeMillis));
    }

    public boolean isTokenRevoked(String jti) {
        String key = BLACK_LIST_PREFIX + "tokens:" + jti;
        boolean revoked = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (revoked) {
            log.warn("[SECURITY ALERT] Попытка использования отозванного токена JTI: {}", jti);
        }
        return revoked;
    }

    public void blacklistUser(String userId) {
        String key = BLACK_LIST_PREFIX + "userid:" + userId;
        log.info("[REDIS BLACKLIST] Блокировка сессий пользователя. UUID: {} принудительно разлогинен на 15 минут", userId);
        redisTemplate.opsForValue().set(key, "true", Duration.ofMinutes(15));
    }

    public boolean isUserBlacklisted(String userId) {
        String key = BLACK_LIST_PREFIX + "userid:" + userId;
        boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (isBlacklisted) {
            log.debug("Запрос отклонен: Пользователь UUID {} находится в черном списке сессий", userId);
        }
        return isBlacklisted;
    }
}