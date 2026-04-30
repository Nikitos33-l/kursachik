package org.example.kursach.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.User;
import org.example.kursach.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JWTService {
    private  final SecretKey Key;
    private final UserRepository userRepository;

    public JWTService(@Value("${jwt.secret}") String key, UserRepository userRepository) {
        Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
        this.userRepository = userRepository;
    }

    public String getToken(HttpServletRequest request){
        String head = request.getHeader("Autorization");
        String token = "";
        if(head!=null&&head.startsWith("Bearer ")) {
            token = head.substring(7);
        }
        return token;
    }

    public String createAcesstoken(String email,String role,Long stationId){
        return Jwts.builder().setSubject(email).
                claim("role",role).
                claim("stationId",stationId).
                setIssuedAt(new Date()).
                setExpiration(new Date(System.currentTimeMillis()+1000*60*15))
                .signWith(Key).compact();
    }

    public String createRefreshtoken(String email){
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+1000*60*60*24*23))
                .signWith(Key).compact();
    }

    public String refreshingAcessToken(String refresh_token){
            validateToken(refresh_token);
            String email = getEmail(refresh_token);
            User user = userRepository.findByEmail(email);
            String role = user.getRole().getName();
            Long stationId = user.getWorkplace() != null ? user.getWorkplace().getId() : null;
            return createAcesstoken(email,role,stationId);

    }

    public void validateToken(String token){
            Jwts.parserBuilder().setSigningKey(Key).build().parseClaimsJws(token);
    }

    public String getEmail(String token){
        return Jwts.parserBuilder().setSigningKey(Key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public UserPrincipal getPrincipal(String token){
       Claims claims = Jwts.parserBuilder().
               setSigningKey(Key).build().parseClaimsJws(token).getBody();
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        Long stationId = claims.get("stationId", Long.class);
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));
        return new UserPrincipal(email,stationId,authorities);
    }
}
