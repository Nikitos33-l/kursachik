package org.example.securitycommon;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class AuthHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rolesHeader = request.getHeader("X-User-Roles");
        String userIdHeader = request.getHeader("X-User-Id");
        String emailHeader = request.getHeader("X-User-Email");
        String stationIdHeader = request.getHeader("X-Station-Id");

        // Если нет ролей или ID пользователя, то и аутентифицировать некого — идем дальше
        if (!StringUtils.hasText(rolesHeader) || !StringUtils.hasText(userIdHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader.trim());

            Long stationId = StringUtils.hasText(stationIdHeader)
                    ? Long.parseLong(stationIdHeader.trim())
                    : null;

            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserPrincipal principal = new UserPrincipal(userId, emailHeader, stationId, authorities);

            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (IllegalArgumentException e) {
            log.error("Failed to parse auth headers from gateway: userId={}, stationId={}", userIdHeader, stationIdHeader, e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}