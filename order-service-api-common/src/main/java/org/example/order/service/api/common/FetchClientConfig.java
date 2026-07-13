package org.example.order.service.api.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@AutoConfiguration
@EnableFeignClients(basePackages = "org.example.order.service.api.common")
public class FetchClientConfig {

}
