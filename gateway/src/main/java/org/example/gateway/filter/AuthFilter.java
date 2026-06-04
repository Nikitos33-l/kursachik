package org.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.security.service.api.common.client.SecurityServiceClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final SecurityServiceClient securityServiceClient;

    public AuthFilter(SecurityServiceClient securityServiceClient) {
        super(Config.class);
        this.securityServiceClient = securityServiceClient;
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                return onError(exchange,HttpStatus.UNAUTHORIZED);
            }
            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            return securityServiceClient.validateToken(authHeader).
                    flatMap(result->{
                        if(result.isValid()){
                            ServerHttpRequest mutateRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id",result.userId().toString())
                                    .header("X-User-Email",result.email())
                                    .header("X-User-Roles", String.join(",", result.roles()))
                                    .header("X-Station-Id",result.stationId() != null ? result.stationId().toString() : "")
                                    .build();
                            return chain.filter(exchange.mutate().request(mutateRequest).build());
                        }
                        else {
                            return onError(exchange,HttpStatus.UNAUTHORIZED);
                        }
                    })
                    .onErrorResume(error->{
                        if (error instanceof WebClientResponseException ex) {
                            log.error("Ошибка от security-service: {}", ex.getStatusCode());
                            return onError(exchange, (HttpStatus) ex.getStatusCode());
                        }
                        log.error("Критическая ошибка при связи с security-service", error);
                        return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
                            }

                    );
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config{
        public Config(){}
    }
}
