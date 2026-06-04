package org.example.gateway.config;

import org.example.security.service.api.common.client.SecurityServiceClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder webClientBuilder(){
        return WebClient.builder();
    }

    @Bean
    SecurityServiceClient securityServiceClient(WebClient.Builder builder){
        WebClient webClient = builder.baseUrl("http://security-service").build();

        WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(webClientAdapter).build();

        return httpServiceProxyFactory.createClient(SecurityServiceClient.class);
    }
}
