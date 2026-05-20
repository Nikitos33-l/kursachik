package org.example.security.service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.security.service.api.common.dto.TokenValidationResultDto;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.User;
import org.example.security.service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.*;

@Service
public class JwtService {

    private final Key signingKey;
    private final TokenBlackListService blacklistService;
    private final UserRepository userRepository;

    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 5;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            TokenBlackListService blacklistService,
            UserRepository userRepository
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.blacklistService = blacklistService;
        this.userRepository = userRepository;
    }

    public String createAccessToken(UUID userId, String email, String role, Long stationId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role);
        claims.put("stationId", stationId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenValidationResultDto validateToken(String token){
        try {
            Claims claims = extractAllClaims(token);

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            String userIdStr = claims.get("userId", String.class);
            UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

            Long stationId = claims.get("stationId", Long.class);

            return new TokenValidationResultDto(true, userId, email, List.of(role), stationId);
        }
        catch (JwtException | IllegalArgumentException e){
            return TokenValidationResultDto.invalid();
        }
    }

    public TokenPair refreshTokens(String refreshToken) {
        try {
            Claims claims = extractAllClaims(refreshToken);
            String jti = claims.getId();
            String email = claims.getSubject();

            if (blacklistService.isRevoked(jti)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has been revoked. Possible theft detected!");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));

            long remainingTime = Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
            blacklistService.blacklistToken(jti, remainingTime);

            String newAccessToken = createAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().getName(),
                    user.getWorkplaceId()
            );
            String newRefreshToken = createRefreshToken(user.getEmail());

            return new TokenPair(newAccessToken, newRefreshToken);

        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}