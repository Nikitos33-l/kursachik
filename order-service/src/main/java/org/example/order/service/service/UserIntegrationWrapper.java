package org.example.order.service.service;

import lombok.RequiredArgsConstructor;
import org.example.user.api.client.UserServiceFeignClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.example.order.service.constant.CacheNames.*;

@Component
@RequiredArgsConstructor
public class UserIntegrationWrapper {

    private final UserServiceFeignClient userServiceFeignClient;

    @Cacheable(value = USER_EMAIL_CACHE,key = "#clientId")
    public String getEmailByUserId(UUID clientId){
        return userServiceFeignClient.getEmailByUserId(clientId);
    }

    @CacheEvict(value = USER_EMAIL_CACHE,key = "#id")
    public void evictCache(UUID id){};

}
