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
    private static final String SECRET_KEY = "Rain_Run_Secret_Key_For_JWT_Token_Generation_And_Verification_Must_Be_Long_Enough";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;
    
    /**
    * 获取 JWT 签名密钥。
    */
    private static SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
    * 生成 JWT token。
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

    /**
    * 解析 JWT token。
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

    /**
    * 校验 token 是否有效。
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

    /**
    * 从 token 中提取用户 ID。
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
    * 从 token 中提取用户名。
    */
    public static String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
