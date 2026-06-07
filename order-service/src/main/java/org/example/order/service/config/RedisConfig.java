package org.example.order.service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

import static org.example.order.service.constant.CacheNames.*;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory){
        RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        RedisCacheConfiguration stationValidationCfg = defaultCfg.entryTtl(Duration.ofHours(24));
        RedisCacheConfiguration userValidationConfig = defaultCfg.entryTtl(Duration.ofHours(24));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCfg)
                .withCacheConfiguration(STATION_VALIDATION_CACHE, stationValidationCfg)
                .withCacheConfiguration(USER_EMAIL_CACHE,userValidationConfig)
                .build();
    }
}