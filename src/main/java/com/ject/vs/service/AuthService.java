package com.ject.vs.service;

import com.ject.vs.domain.Token;
import com.ject.vs.domain.TokenType;
import com.ject.vs.domain.User;
import com.ject.vs.dto.LoginTokenResponse;
import com.ject.vs.dto.TokenInfo;
import com.ject.vs.repository.TokenRepository;
import com.ject.vs.util.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserService userService;
    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;

    public LoginTokenResponse socialLogin(String sub) {
        User user = userService.findOrCreate(sub);

        TokenInfo accessTokenInfo = jwtProvider.createAccessToken(user.getId());
        TokenInfo refreshTokenInfo = jwtProvider.createRefreshToken(user.getId());

        Token accessToken = Token.builder()
                .user(user)
                .tokenValue(accessTokenInfo.getTokenValue())
                .tokenType(accessTokenInfo.getTokenType())
                .expiresAt(accessTokenInfo.getExpiresAt())
                .revoked(false)
                .build();

        Token refreshToken = Token.builder()
                .user(user)
                .tokenValue(refreshTokenInfo.getTokenValue())
                .tokenType(refreshTokenInfo.getTokenType())
                .expiresAt(refreshTokenInfo.getExpiresAt())
                .revoked(false)
                .build();

        tokenRepository.save(refreshToken);

        return LoginTokenResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken.getTokenValue())
                .refreshToken(refreshToken.getTokenValue())
                .build();
    }

    public TokenInfo reissueAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("토큰이 없습니다.");
        }

        if (!jwtProvider.validationToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String tokenType = jwtProvider.getTokenType(refreshToken);
        if (!TokenType.REFRESH.name().equals(tokenType)) {
            throw new IllegalArgumentException("지정된 토큰 타입이 아닙니다.");
        }

        Token savedRefreshToken = tokenRepository.findByTokenValueAndTokenType(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new IllegalArgumentException("저장된 토큰이 없습니다."));

        if (savedRefreshToken.isRevoked()) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        return jwtProvider.createAccessToken(userId);
    }
}
