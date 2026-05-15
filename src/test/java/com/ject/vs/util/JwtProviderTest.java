package com.ject.vs.util;

import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {
    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void createAccessTokenContainsUserTypeAndExpiration() {
        JwtProvider jwtProvider = jwtProvider(1_800, 86_400);

        TokenInfo tokenInfo = jwtProvider.createAccessToken(1L);
        TokenInfo parsedToken = jwtProvider.parseToken(tokenInfo.tokenValue());

        assertThat(jwtProvider.validateToken(tokenInfo.tokenValue())).isTrue();
        assertThat(jwtProvider.getUserId(tokenInfo.tokenValue())).isEqualTo(1L);
        assertThat(jwtProvider.getTokenType(tokenInfo.tokenValue())).isEqualTo(TokenType.ACCESS.name());
        assertThat(parsedToken.userId()).isEqualTo(1L);
        assertThat(parsedToken.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(parsedToken.expiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
    }

    @Test
    void createRefreshTokenUsesRefreshType() {
        JwtProvider jwtProvider = jwtProvider(1_800, 86_400);

        TokenInfo tokenInfo = jwtProvider.createRefreshToken(2L);
        TokenInfo parsedToken = jwtProvider.parseToken(tokenInfo.tokenValue());

        assertThat(jwtProvider.validateToken(tokenInfo.tokenValue())).isTrue();
        assertThat(parsedToken.userId()).isEqualTo(2L);
        assertThat(parsedToken.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void validateTokenRejectsMissingMalformedAndExpiredTokens() {
        JwtProvider jwtProvider = jwtProvider(-1, 86_400);

        TokenInfo expiredToken = jwtProvider.createAccessToken(1L);

        assertThat(jwtProvider.validateToken(null)).isFalse();
        assertThat(jwtProvider.validateToken(" ")).isFalse();
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
        assertThat(jwtProvider.validateToken(expiredToken.tokenValue())).isFalse();
    }

    private JwtProvider jwtProvider(long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds) {
        JwtProvider jwtProvider = new JwtProvider(
                new JwtProperties(SECRET, accessTokenExpirationSeconds, refreshTokenExpirationSeconds)
        );
        jwtProvider.init();
        return jwtProvider;
    }
}
