package org.example.security.service.api.common.client;

import org.example.security.service.api.common.dto.TokenValidationResultDto;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/api/token/internal/validate")
public interface SecurityServiceClient {

    @PostExchange
    Mono<TokenValidationResultDto> validateToken(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authHeader);


}
