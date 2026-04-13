package org.example.dln.config;

import org.example.dln.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 包名：org.example.dln.config
 * 类名：WebConfig
 * 类描述：配置 Spring MVC 拦截器与跨域等 Web 能力。
 * 创建人：@author Rain_润
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;
    
    /**
    * 注册 MVC 拦截器。
    */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/avatars/**");
    }

    /**
    * 配置跨域映射。
    */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
    * 注册静态资源处理规则。
    */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarLocation = Paths.get(avatarUploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
