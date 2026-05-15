package com.ject.vs.util;

import com.ject.vs.auth.domain.TokenStatus;
import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.config.JwtProperties;
import com.ject.vs.user.domain.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtProviderTest {
    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void createAccessTokenContainsUserTypeAndExpiration() {
        JwtProvider jwtProvider = jwtProvider(1_800, 86_400);

        TokenInfo tokenInfo = jwtProvider.createAccessToken(1L);
        TokenInfo parsedToken = jwtProvider.parseToken(tokenInfo.tokenValue());

        assertThat(jwtProvider.validationToken(tokenInfo.tokenValue())).isEqualTo(TokenStatus.VALID);
        assertThat(jwtProvider.getUserId(tokenInfo.tokenValue())).isEqualTo(1L);
        assertThat(jwtProvider.getTokenType(tokenInfo.tokenValue())).isEqualTo(TokenType.ACCESS.name());
        assertThat(parsedToken.userId()).isEqualTo(1L);
        assertThat(parsedToken.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(parsedToken.expiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
    }

    @Test
    void createRefreshTokenUsesRefreshType() {
        JwtProvider jwtProvider = jwtProvider(1_800, 86_400);

        TokenInfo tokenInfo = jwtProvider.createRefreshToken(2L);
        TokenInfo parsedToken = jwtProvider.parseToken(tokenInfo.tokenValue());

        assertThat(jwtProvider.validationToken(tokenInfo.tokenValue())).isEqualTo(TokenStatus.VALID);
        assertThat(parsedToken.userId()).isEqualTo(2L);
        assertThat(parsedToken.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void validateTokenRejectsMissingMalformedAndExpiredTokens() {
        JwtProvider jwtProvider = jwtProvider(-1, 86_400);

        TokenInfo expiredToken = jwtProvider.createAccessToken(1L);

        assertThat(jwtProvider.validationToken(null)).isEqualTo(TokenStatus.EMPTY);
        assertThat(jwtProvider.validationToken(" ")).isEqualTo(TokenStatus.EMPTY);
        assertThat(jwtProvider.validationToken("not-a-jwt")).isEqualTo(TokenStatus.INVALID);
        assertThat(jwtProvider.validationToken(expiredToken.tokenValue())).isEqualTo(TokenStatus.EXPIRED);
    }

    private JwtProvider jwtProvider(long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds) {
        JwtProvider jwtProvider = new JwtProvider(
                mock(UserRepository.class),
                new JwtProperties(SECRET, accessTokenExpirationSeconds, refreshTokenExpirationSeconds)
        );
        jwtProvider.init();
        return jwtProvider;
    }
}
