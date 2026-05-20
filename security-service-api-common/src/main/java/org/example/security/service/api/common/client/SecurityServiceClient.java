package org.example.security.service.api.common.client;

import org.example.security.service.api.common.dto.TokenValidationResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "security-service")
public interface SecurityServiceClient {

    @PostMapping("/api/token/internal/validate")
    TokenValidationResultDto validateToken(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authHeader);


}
