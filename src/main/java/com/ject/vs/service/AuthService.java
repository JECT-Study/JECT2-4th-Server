package com.ject.vs.service;

import com.ject.vs.domain.Token;
import com.ject.vs.domain.TokenStatus;
import com.ject.vs.domain.TokenType;
import com.ject.vs.domain.User;
import com.ject.vs.dto.LoginTokenResponse;
import com.ject.vs.dto.TokenInfo;
import com.ject.vs.dto.TokenReissueResponse;
import com.ject.vs.exception.CustomException;
import com.ject.vs.exception.ErrorCode;
import com.ject.vs.repository.TokenRepository;
import com.ject.vs.util.JwtProvider;
import com.nimbusds.oauth2.sdk.TokenResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .tokenValue(accessTokenInfo.tokenValue())
                .tokenType(accessTokenInfo.tokenType())
                .expiresAt(accessTokenInfo.expiresAt())
                .revoked(false)
                .build();

        Token refreshToken = Token.builder()
                .user(user)
                .tokenValue(refreshTokenInfo.tokenValue())
                .tokenType(refreshTokenInfo.tokenType())
                .expiresAt(refreshTokenInfo.expiresAt())
                .revoked(false)
                .build();

        tokenRepository.save(refreshToken);

        return LoginTokenResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken.getTokenValue())
                .refreshToken(refreshToken.getTokenValue())
                .build();
    }

    public void handleTokenStatus(TokenStatus status) {
        switch (status) {
            case EMPTY -> throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            case EXPIRED -> throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            case INVALID -> throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public TokenReissueResponse reissueAccessToken(String refreshToken) {
        TokenStatus status = jwtProvider.validationToken(refreshToken);
        handleTokenStatus(status);

        // db에서 토큰 있는지 확인
        Token savedToken = tokenRepository.findByTokenValueAndTokenType(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

        // 회수된 토큰인지 확인
        if (savedToken.isRevoked()) {
            throw new CustomException(ErrorCode.REVOKED_TOKEN);
        }

        // 기존 토큰 회수
        savedToken.revoke();

        User user = jwtProvider.getUser(refreshToken);

        // 5. 새로운 토큰 세트 생성
        TokenInfo newAccessToken = jwtProvider.createAccessToken(user.getId());
        TokenInfo newRefreshToken = jwtProvider.createRefreshToken(user.getId());

        Token newRefreshTokenInfo = Token.builder()
                .user(user)
                .tokenValue(newRefreshToken.tokenValue())
                .tokenType(newRefreshToken.tokenType())
                .expiresAt(newRefreshToken.expiresAt())
                .revoked(false)
                .build();

        tokenRepository.save(newRefreshTokenInfo);

        return TokenReissueResponse.from(newRefreshTokenInfo, newAccessToken.tokenValue());
    }
}
