package org.example.order.service.api.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {
    @DeleteMapping("/delete/by/userId/{id}")
    void deleteByUser(@PathVariable Long clientId);
}
