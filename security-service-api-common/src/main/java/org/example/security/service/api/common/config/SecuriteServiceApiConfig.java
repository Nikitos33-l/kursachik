package org.example.security.service.api.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@AutoConfiguration
@EnableFeignClients(basePackages = "org.example.security.service.api.common.client")
public class SecuriteServiceApiConfig {
}
