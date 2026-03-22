package org.example.dln.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 包名称：org.example.dln.util
 * 类名称：JwtUtil
 * 类描述：Jwt 工具类，用于生成和验证 JWT
 * 创建人：@author Rain_润
 * 创建时间：2026-03-20 14:28
 */
public class JwtUtil {
    private static final String SECRET_KEY = "Rain_Run_Secret_Key_For_JWT_Token_Generation_And_Verification_Must_Be_Long_Enough";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;
    
    private static SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .id(userId.toString())
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(getSigningKey())
                .compact();
    }

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

    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration != null && !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        String id = claims.getId();
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("用户 ID 格式错误", e);
        }
    }

    public static String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}