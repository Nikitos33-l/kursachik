package org.example.station.service.api.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@AutoConfiguration
@EnableFeignClients(basePackages = "org.example.station.service.api.common.client")
public class StationServiceClientConfig {

}
