package com.ject.vs.util;

import com.ject.vs.config.JwtProperties;
import com.ject.vs.domain.TokenType;
import com.ject.vs.dto.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {        // secretkey 값을 읽어 디코딩 하여 바이트 배열로 변환 후 hmac으로 암호화
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenInfo createAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, TokenType.ACCESS, LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()), userId);
    }

    public TokenInfo createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, TokenType.REFRESH, LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()), userId);
    }

    public boolean validationToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getTokenType(String token) {
        Object type = getClaims(token).get("type");
        return type == null ? null : type.toString();
    }

    public TokenInfo parseToken(String token) {
        Claims claims = getClaims(token);
        String type = claims.get("type").toString();
        Long userId = Long.parseLong(claims.getSubject());
        return new TokenInfo(token, TokenType.valueOf(type), LocalDateTime.now(), userId);
    }
}
