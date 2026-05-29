package org.example.dln.config;

import org.example.dln.security.CurrentUserIdArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.nio.file.Paths;
import java.util.List;

/**
 * 包名：org.example.dln.config
 * 类名：WebConfig
 * 类描述：配置 Web 应用。
 * 创建人：@author Rain_润
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    /**
    * 注册控制器参数解析器。
     * @param resolvers 参数解析器列表
    */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
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
