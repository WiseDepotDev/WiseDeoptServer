package com.huicang.wise.infrastructure.security;

import com.huicang.wise.domain.auth.port.TokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider implements TokenVerifier {

    /** JWT 签名密钥。STD-SEC-01：不提供默认值，未配置时启动即失败，避免退化到公开弱密钥。 HS512 要求密钥长度 ≥ 64 字节。 */
    private static final int MIN_SECRET_BYTES = 64;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.access:7200}")
    private long accessTokenExpiration;

    @Value("${jwt.expiration.refresh:604800}")
    private long refreshTokenExpiration;

    @jakarta.annotation.PostConstruct
    void validateSecret() {
        int length =
                jwtSecret == null
                        ? 0
                        : jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 长度不足：当前 "
                            + length
                            + " 字节，HS512 要求至少 "
                            + MIN_SECRET_BYTES
                            + " 字节。请通过环境变量 WISE_JWT_SECRET 或 config/application-local.yml 配置。");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Long userId) {
        return generateToken(username, userId, "access", accessTokenExpiration);
    }

    public String generateRefreshToken(String username, Long userId) {
        return generateToken(username, userId, "refresh", refreshTokenExpiration);
    }

    private String generateToken(String username, Long userId, String type, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        claims.put("username", username);
        claims.put("userId", userId);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims =
                Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .setAllowedClockSkewSeconds(60)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims =
                Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .setAllowedClockSkewSeconds(60)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || "undefined".equals(token)) {
                return false;
            }
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .setAllowedClockSkewSeconds(60) // 允许60秒的时钟偏移
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenType(String token, String expectedType) {
        try {
            Claims claims =
                    Jwts.parserBuilder()
                            .setSigningKey(getSigningKey())
                            .setAllowedClockSkewSeconds(60)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

            String tokenType = claims.get("type", String.class);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            log.error("JWT令牌类型验证失败: {}", e.getMessage());
            return false;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims =
                Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
