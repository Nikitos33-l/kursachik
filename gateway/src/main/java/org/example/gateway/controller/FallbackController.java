package org.example.gateway.controller;

import org.example.gateway.dto.FallbackResponse;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/forward")
public class FallbackController {

    @RequestMapping("/security")
    public Mono<ResponseEntity<FallbackResponse>> securityFallback(ServerWebExchange exchange){
        return Mono.just(ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new FallbackResponse("Сервис авторизации временно недоступен, попробуйте позже",
                                getExceptionMessage(exchange)
                                ))
        );
    }

    @RequestMapping("/order")
    public Mono<ResponseEntity<FallbackResponse>> orderFallback(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Сервис заказов СТО не отвечает или перегружен. Мы уже чиним его!",
                        getExceptionMessage(exchange)
                )));
    }

    @RequestMapping("/station")
    public Mono<ResponseEntity<FallbackResponse>> stationFallback(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Не удалось связаться с сервисом станций и услуг.",
                        getExceptionMessage(exchange)
                )));
    }

    @RequestMapping("/user")
    public Mono<ResponseEntity<FallbackResponse>> userFallback(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        "Профиль пользователя или данные авто временно недоступны.",
                        getExceptionMessage(exchange)
                )));
    }

    private String getExceptionMessage(ServerWebExchange exchange) {
        Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        if (exception != null) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
        return "Unknown failure (Timeout or Instance dropped)";
    }
}
