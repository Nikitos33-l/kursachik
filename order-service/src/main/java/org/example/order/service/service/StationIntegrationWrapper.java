package org.example.order.service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.api.common.client.StationServiceClient;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.order.service.constant.CacheNames.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationIntegrationWrapper {

    private final StationServiceClient stationClient;
    private final StringRedisTemplate redisTemplate;

    @Cacheable(
            value = STATION_VALIDATION_CACHE,
            key = "T(org.example.order.service.service.StationIntegrationWrapper).generateCacheKey(#stationId, #serviceIds)",
            unless = "!#result.stationExists"

    )
    @CircuitBreaker(name = "stationServiceBreaker", fallbackMethod = "getValidatedServicesFallback")
    public StationServicesResponse getValidatedServices(Long stationId, List<Long> serviceIds) {
        log.info("[CACHE MISS] Запрос валидации услуг СТО ID: {} через Station Service (Feign)", stationId);
        return stationClient.validateStationAndGetServices(stationId, serviceIds);
    }


    public StationServicesResponse getValidatedServicesFallback(Long stationId, List<Long> serviceIds, Throwable throwable) {
        log.error("[FALLBACK] Не удалось провалидировать услуги для СТО ID: {}. Сервис недоступен. Причина: {}",
                stationId, throwable.getMessage());

        return new StationServicesResponse(false, Collections.emptyList());
    }

    public static String generateCacheKey(Long stationId, List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return "station:" + stationId + ":services:none";
        }
        String sortedIds = serviceIds.stream()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return "station:" + stationId + ":services:[" + sortedIds + "]";
    }

    public void evictCache(Long stationId) {
        String pattern = "order-service:station-validation::station:" + stationId + ":services:*";
        log.info("[REDIS CLEANUP] Поиск ключей валидации услуг СТО по шаблону: {}", pattern);

        Set<String> keysToDelete = redisTemplate.keys(pattern);
        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            log.info("[REDIS CLEANUP] Найдено и удалено {} ключей кэша для СТО ID: {}", keysToDelete.size(), stationId);
            redisTemplate.delete(keysToDelete);
        } else {
            log.debug("[REDIS CLEANUP] Активных ключей кэша для СТО ID: {} не обнаружено", stationId);
        }
    }
}