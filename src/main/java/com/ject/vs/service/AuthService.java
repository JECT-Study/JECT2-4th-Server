package com.ject.vs.service;

import com.ject.vs.domain.Token;
import com.ject.vs.domain.TokenType;
import com.ject.vs.domain.User;
import com.ject.vs.dto.LoginTokenResponse;
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

        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = jwtProvider.createAccesssToken(user.getId(), accessTokenId);
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), refreshTokenId);

        LocalDateTime accessExpiresAt = jwtProvider.getClaims(accessToken)
                .getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime refreshExpiresAt = jwtProvider.getClaims(refreshToken)
                .getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Token accesTk = Token.builder()
                .tokenId(accessTokenId)
                .user(user)
                .tokenValue(accessToken)
                .tokenType(TokenType.ACCESS)
                .expiresAt(accessExpiresAt)
                .revoked(false)
                .build();

        Token refreshTk = Token.builder()
                .tokenId(refreshTokenId)
                .user(user)
                .tokenValue(refreshToken)
                .tokenType(TokenType.REFRESH)
                .expiresAt(refreshExpiresAt)
                .revoked(false)
                .build();

        tokenRepository.save(accesTk);
        tokenRepository.save(refreshTk);

        return LoginTokenResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
