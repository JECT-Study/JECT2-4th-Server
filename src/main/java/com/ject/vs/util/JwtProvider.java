package com.ject.vs.util;

import com.ject.vs.auth.domain.TokenStatus;
import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.exception.TokenErrorCode;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.config.JwtProperties;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.exception.UserErrorCode;
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
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
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

    public TokenStatus validationToken(String token) {
        if(token == null || token.isBlank()) return TokenStatus.EMPTY;
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return TokenStatus.VALID;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenStatus.INVALID;
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(TokenErrorCode.EXPIRED_TOKEN);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new BusinessException(TokenErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public User getUser(String token) {
        return userRepository.findById(getUserId(token)).orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
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
