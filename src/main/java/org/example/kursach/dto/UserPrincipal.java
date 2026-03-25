package org.example.kursach.dto;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record UserPrincipal(
        String email,
        Long stationId,
        Collection<? extends GrantedAuthority> authorities
) {
    public String getName() {
        return email;
    }
}
