package org.example.dln.config;

import org.example.dln.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    //添加JWT拦截器
    /**
    * 注册 Web 拦截器。
     * @param registry 拦截器注册器
    */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/avatars/**");
    }

    //配置静态资源处理器，作为映射处理头像
    /**
    * 注册静态资源处理器。
     * @param registry 静态资源处理器注册器
    */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarLocation = Paths.get(avatarUploadDir)
                                     .toAbsolutePath()
                                     .normalize()
                                     .toUri()
                                     .toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
