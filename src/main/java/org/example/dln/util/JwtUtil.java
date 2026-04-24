package org.example.dln.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 包名：org.example.dln.util
 * 类名：JwtUtil
 * 类描述：提供 JWT 生成、解析与校验工具方法。
 * 创建人：@author Rain_润
 */
public class JwtUtil {
    /**
     * JWT由三部分组成：
     * Header（头部）：算法和类型
     * Payload（载荷）：即 Claims，存储实际数据
     * Signature（签名）：验证完整性
     */

    //密钥必须至少256位
    private static final String SECRET_KEY = "Rain_Run_Secret_Key_For_JWT_Token_Generation_And_Verification_Must_Be_Long_Enough";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;

    //生成签名密钥
    /**
    * 获取 JWT 签名密钥。
    */
    private static SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //生成
    /**
    * 生成 JWT 令牌。
     * @param userId 用户ID
     * @param username 用户名
    */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .id(userId.toString())
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    //解析
    /**
    * 解析 JWT 令牌。
     * @param token JWT令牌
    */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("token 已过期", e);
        } catch (Exception e) {
            throw new RuntimeException("token 无效或格式错误", e);
        }
    }

    //验证有效性
    /**
    * 校验 JWT 令牌是否有效。
     * @param token JWT令牌
    */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration != null && !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    //拿用户ID和用户名
    /**
    * 从 JWT 令牌中提取用户 ID。
     * @param token JWT令牌
    */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        String id = claims.getId();
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("用户 ID 格式错误", e);
        }
    }

    /**
    * 从 JWT 令牌中提取用户名。
     * @param token JWT令牌
    */
    public static String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
