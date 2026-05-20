package org.example.security.service.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlackListService {
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public void blacklistToken(String jti, long remainingTimeMillis) {
        revokedTokens.add(jti);

    }

    public boolean isRevoked(String jti) {
        return revokedTokens.contains(jti);
    }
}
