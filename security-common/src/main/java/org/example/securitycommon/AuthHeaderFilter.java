package org.example.securitycommon;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

public class AuthHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String stationId = request.getHeader("X-Station-Id");
        String email = request.getHeader("X-User-Email");
        String roles = request.getHeader("X-User-Roles");

        if (roles != null) {
            var authorities = Arrays.stream(roles.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserPrincipal principal = new UserPrincipal(email,
                    stationId != null ? Long.parseLong(stationId) : null,
                    authorities
            );
            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request,response);
    }
}
