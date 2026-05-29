package org.example.dln.config;

import org.example.dln.security.JwtAuthenticationFilter;
import org.example.dln.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 包名：org.example.dln.config
 * 类名：SecurityConfig
 * 类描述：Spring Security 配置类。
 * 创建人：@author Rain_Run
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    /**
    * 创建密码编码器。
    */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
    * 创建跨域配置源。
    */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //设置来源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        //设置请求方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        //设置请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        //设置响应头
        configuration.setExposedHeaders(Arrays.asList("*"));
        //设置是否带着凭证，用JWT实现，不需要
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //注册配置，匹配所有路径
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
    * 配置 Spring Security 过滤链。每个请求先在这里处理，禁用传统的Web安全机制CSRF、登录表单、Session
     * @param http Spring Security HTTP配置对象
     * @throws Exception 配置过程中发生异常时抛出
    */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //禁用CSRF，前端通过 token 认证，不依赖 session
        http.csrf(AbstractHttpConfigurer::disable)
                //不使用自带的表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                //禁用HTTP基本认证
                .httpBasic(AbstractHttpConfigurer::disable)
                //跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //设置无状态模式，每次请求都必须带JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //配置异常处理入口
                .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
                // 配置规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/login", "/register", "/avatars/**").permitAll()
                        //除了上述允许的路径外，所有其他请求都必须经过认证
                        .anyRequest().authenticated()
                )
                //添加JWT过滤器，从请求头中提取JWT并解析
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
