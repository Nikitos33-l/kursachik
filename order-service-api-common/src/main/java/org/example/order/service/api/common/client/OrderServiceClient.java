package org.example.order.service.api.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @DeleteMapping("/api/order/internal/delete/by/stationId/{id}")
    void deleteByStation(@PathVariable Long id);
}
