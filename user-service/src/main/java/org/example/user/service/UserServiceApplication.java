package org.example.user.service;

import org.example.user.api.FetchClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {FetchClientConfig.class,
org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration.class
})
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
