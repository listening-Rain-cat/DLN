package org.example.dln.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * 包名：org.example.dln.config
 * 类名：RedisCacheConfig
 * 类描述：配置 Redis 缓存的序列化方式和默认过期时间。
 * 创建人：@author Rain_润
 */
@Configuration
public class RedisCacheConfig {
    /**
    * 自定义 Redis 缓存管理器。
     * @param objectMapper JSON 对象映射器
    */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType("org.example.dln")
                        .allowIfBaseType("java.util")
                        .allowIfSubType("org.example.dln")
                        .allowIfSubType("java.util")
                        .allowIfSubType("java.time")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .prefixCacheNameWith("dln:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return builder -> builder.cacheDefaults(cacheConfiguration);
    }
}
