package com.ject.vs.util;

import com.ject.vs.config.JwtProperties;
import com.ject.vs.domain.TokenType;
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
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void Init() {        // secretkey 값을 읽어 디코딩 하여 바이트 배열로 변환 후 hmac으로 암호화
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccesssToken(Long userId, String tokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(tokenId)
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String tokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(tokenId)
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
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

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public String getTokenType(String token) {
        Object type = getClaims(token).get("type");
        return type == null ? null : type.toString();
    }
}
