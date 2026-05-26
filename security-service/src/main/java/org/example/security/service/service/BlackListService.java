package org.example.security.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BlackListService {

    private final StringRedisTemplate redisTemplate;

    private final String BLACK_LIST_PREFIX = "security-service:blacklist:";

    private final Set<String> blacklistedUsers = ConcurrentHashMap.newKeySet();

    public void blacklistToken(String jti, long remainingTimeMillis) {
        redisTemplate.opsForValue().set(BLACK_LIST_PREFIX+"tokens:"+jti,"true", Duration.ofMillis(remainingTimeMillis));
    }

    public boolean isTokenRevoked(String jti) {
        return redisTemplate.hasKey(BLACK_LIST_PREFIX + "tokens:" + jti);
    }

    public void blacklistUser(String userId) {
        redisTemplate.opsForValue().set(BLACK_LIST_PREFIX + "userid:"+ userId,"true",Duration.ofMinutes(15));
    }

    public boolean isUserBlacklisted(String userId) {
        return redisTemplate.hasKey(BLACK_LIST_PREFIX + "userid:"+ userId);
    }
}