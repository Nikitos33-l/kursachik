package org.example.security.service.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlackListService {

    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> blacklistedUsers = ConcurrentHashMap.newKeySet();

    public void blacklistToken(String jti, long remainingTimeMillis) {
        revokedTokens.add(jti);
    }

    public boolean isTokenRevoked(String jti) {
        return revokedTokens.contains(jti);
    }

    public void blacklistUser(String userId) {
        blacklistedUsers.add(userId);
    }

    public boolean isUserBlacklisted(String userId) {
        return blacklistedUsers.contains(userId);
    }
}