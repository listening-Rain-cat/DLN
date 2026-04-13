package org.example.dln.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 包名：org.example.dln.config
 * 类名：JacksonConfig
 * 类描述：配置 Jackson 的 JSON 序列化与反序列化行为。
 * 创建人：@author Rain_润
 */
@Configuration
public class JacksonConfig {
    /**
    * 自定义 Jackson 对象映射器构建器。
    */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
