package org.example.dln.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.dln.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 包名：org.example.dln.interceptor
 * 类名：JwtInterceptor
 * 类描述：实现基于 JWT 的请求鉴权拦截逻辑。
 * 创建人：@author Rain_润
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final String PRIMARY_TOKEN_COOKIE = "dln-token";
    private static final String LEGACY_TOKEN_COOKIE = "token";

    /**
    * 写入跨域响应头。
    */
    private void writeCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin == null || origin.isBlank() ? "*" : origin);
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Expose-Headers", "*");
    }

    /**
    * 在请求进入控制器前执行鉴权处理。
    */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod
                && !handlerMethod.getBeanType().getName().contains("controller")) {
            return true;
        }

        writeCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if ("/login".equals(path) || "/register".equals(path)) {
            log.debug("放行公开接口: {}", path);
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少 token: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\",\"data\":null}");
            return false;
        }

        if (!JwtUtil.validateToken(token)) {
            log.warn("token 无效或已过期: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token 无效或已过期\",\"data\":null}");
            return false;
        }

        Long userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        log.debug("token 校验通过, userId={}, username={}", userId, username);
        return true;
    }

    /**
    * 解析请求中的 token。
    */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            return stripBearerPrefix(authorization);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "";
        }

        for (Cookie cookie : cookies) {
            if (cookie == null) {
                continue;
            }

            String name = cookie.getName();
            if (PRIMARY_TOKEN_COOKIE.equals(name) || LEGACY_TOKEN_COOKIE.equals(name)) {
                String value = cookie.getValue();
                if (StringUtils.hasText(value)) {
                    return stripBearerPrefix(value.trim());
                }
            }
        }

        return "";
    }

    /**
    * 去除 Bearer 前缀。
    */
    private String stripBearerPrefix(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.startsWith("Bearer ")) {
            return normalized.substring(7);
        }
        return normalized;
    }
}
