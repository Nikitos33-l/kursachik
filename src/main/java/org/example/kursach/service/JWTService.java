package org.example.kursach.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.kursach.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
@Component
public class JWTService {
    private  final SecretKey Key;
    private UserRepository userRepository;
    public JWTService(@Value("${jwt.secret}") String key, UserRepository userRepository) {
        Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
        this.userRepository = userRepository;
    }
    public String get_token(HttpServletRequest request){
        String head = request.getHeader("Autorization");
        String token = "";
        if(head!=null&&head.startsWith("Bearer ")) {
            token = head.substring(7);
        }
        return token;
    }
    public String createAcesstoken(String email,String role){
        return Jwts.builder().setSubject(email).
                claim("role",role).
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
    public String refreshing_acess_token(String refresh_token){
            validate_token(refresh_token);
            String email = get_email(refresh_token);
            String role = userRepository.findByEmail(email).getRole().getName();
            return createAcesstoken(email,role);

    }
    public boolean validate_token(String token){
            Jwts.parserBuilder().setSigningKey(Key).build().parseClaimsJws(token);
            return true;
    }
    public String get_email(String token){
        return Jwts.parserBuilder().setSigningKey(Key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public String get_role(String token) {
        return Jwts.parserBuilder().setSigningKey(Key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }
}
