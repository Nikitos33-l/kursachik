package org.example.kursach.filter;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.kursach.service.JWTService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    public JwtFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.equals("/api/user/login") || path.equals("/api/user/register") || path.equals("/api/token/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        String head=request.getHeader("Autorization");
        if(head!=null&&head.startsWith("Bearer ")){
            String token=head.substring(7);
            System.out.println(token);
            try {
                jwtService.validate_token(token);
                System.out.println("Токен впорядке");
                List<GrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority(jwtService.get_role(token)));
                Authentication authentication = new UsernamePasswordAuthenticationToken(jwtService.get_email(token),null,authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request,response);
            }
            catch (ExpiredJwtException e) {
                System.out.println("Истек");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"token_expired\"}");
            }
            catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"token_invalid\"}");
            }

        }
    }
}
