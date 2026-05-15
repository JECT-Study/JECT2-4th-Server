package com.ject.vs.auth.port;

import com.ject.vs.auth.domain.Token;
import com.ject.vs.auth.domain.TokenRepository;
import com.ject.vs.auth.domain.TokenStatus;
import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.exception.TokenErrorCode;
import com.ject.vs.auth.port.in.dto.LoginTokenResponse;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.auth.port.in.dto.TokenReissueResponse;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.UserService;
import com.ject.vs.util.JwtProvider;
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

    public LoginTokenResponse socialLogin(String email) {
        User user = userService.findOrCreate(email);

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
                .userStatus(user.getUserStatus())
                .build();
    }

    public void handleTokenStatus(TokenStatus status) {
        switch (status) {
            case EMPTY -> throw new BusinessException(TokenErrorCode.TOKEN_NOT_FOUND);
            case EXPIRED -> throw new BusinessException(TokenErrorCode.REFRESH_TOKEN_EXPIRED);
            case INVALID -> throw new BusinessException(TokenErrorCode.INVALID_TOKEN);
        }
    }

    public TokenReissueResponse reissueAccessToken(String refreshToken) {
        TokenStatus status = jwtProvider.validationToken(refreshToken);
        handleTokenStatus(status);

        Token savedToken = tokenRepository.findByTokenValueAndTokenType(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new BusinessException(TokenErrorCode.TOKEN_NOT_FOUND));

        if (savedToken.isRevoked()) {
            throw new BusinessException(TokenErrorCode.REVOKED_TOKEN);
        }

        savedToken.revoke();

        User user = jwtProvider.getUser(refreshToken);

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
