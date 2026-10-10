package com.life.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析 Token（H1 鉴权机制的落地点）。
 *
 * <p>载荷：sub = userId，username = 用户名；签名算法 HS256。
 * secret 与过期时间可经配置覆盖（jwt.secret / jwt.expire，单位毫秒）。</p>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:life-manager-default-secret-key-please-change-in-production-2026}")
    private String secret;

    /** 过期时间，默认 24 小时 */
    @Value("${jwt.expire:86400000}")
    private long expireMs;

    private SecretKey key() {
        // HS256 要求密钥长度 >= 256 bit（32 字节）
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getUsername(String token) {
        Object v = parse(token).get("username");
        return v == null ? null : v.toString();
    }
}
