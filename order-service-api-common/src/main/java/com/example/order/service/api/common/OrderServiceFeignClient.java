package com.example.order.service.api.common;

import com.example.order.service.api.common.dto.OrderTotalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceFeignClient {
    @GetMapping("/api/order/internal/total/{orderId}")
    OrderTotalResponse getTotalByOrderId(@PathVariable Long orderId);
}
