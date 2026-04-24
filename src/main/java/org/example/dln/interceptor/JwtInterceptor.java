package org.example.dln.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dln.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 参考文档：https://juejin.cn/post/7527846239939215395 https://blog.csdn.net/2301_80216352/article/details/155058409
 * 客户端请求 → 过滤器(Filter) → 拦截器(Interceptor) → 控制器(Controller) → 服务层(Service) → 数据库
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final String PRIMARY_TOKEN_COOKIE = "dln-token";
    private static final String LEGACY_TOKEN_COOKIE = "token";

    //JWT在请求头中的Authorization字段，且有前缀Bearer，或者在Cookie中存储，优先从请求头获取
    /**
    * 去除 Bearer 前缀。
     * @param token 原始令牌字符串
    */
    private String stripBearerPrefix(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.startsWith("Bearer ")) {
            return normalized.substring(7); // 获取纯payload
        }
        return normalized;
    }

    // 提取
    /**
    * 从请求中解析 JWT 令牌。
     * @param request HTTP请求对象
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
    * 预处理请求并校验登录状态。
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 当前处理器对象
     * @throws Exception 响应写出失败时抛出
    */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod
                && !handlerMethod.getBeanType().getName().contains("controller")) {
            return true;
        }

        String path = request.getRequestURI(); // 获取请求路径
        if ("/login".equals(path) || "/register".equals(path)) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) { // 如果令牌为空
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8"); // 设置响应内容类型
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\",\"data\":null}");
            return false;
        }

        if (!JwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token 无效或已过期\",\"data\":null}");
            return false;
        }

        Long userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        return true;
    }


}
