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
import com.ject.vs.user.domain.UserStatus;
import com.ject.vs.user.port.UserService;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    private static final Long USER_ID = 1L;
    private static final String FAMILY = "test-family-uuid";

    private User createUser() {
        return User.createWithEmail("test@example.com");
    }

    private User createUserWithId(Long id) {
        User user = User.createWithEmail("test@example.com");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private TokenInfo createTokenInfo(TokenType type, Instant expiresAt) {
        return new TokenInfo("token-value", type, expiresAt, USER_ID);
    }

    @Nested
    @DisplayName("socialLogin")
    class SocialLogin {

        @Test
        @DisplayName("소셜 로그인 성공 시 Refresh Token에 tokenFamily가 설정된다")
        void socialLogin_setsFamilyOnRefreshToken() {
            User user = createUserWithId(USER_ID);

            TokenInfo accessInfo = createTokenInfo(TokenType.ACCESS, Instant.now().plusSeconds(3600));
            TokenInfo refreshInfo = createTokenInfo(TokenType.REFRESH, Instant.now().plusSeconds(1209600));

            given(userService.findOrCreate("test@example.com")).willReturn(user);
            given(jwtProvider.createAccessToken(USER_ID)).willReturn(accessInfo);
            given(jwtProvider.createRefreshToken(USER_ID)).willReturn(refreshInfo);

            LoginTokenResponse response = authService.socialLogin("test@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getAccessToken()).isEqualTo(accessInfo.tokenValue());
            assertThat(response.getRefreshToken()).isEqualTo(refreshInfo.tokenValue());

            // refresh token이 저장될 때 family가 설정되었는지 검증
            verify(tokenRepository).save(argThat(token ->
                    token.getTokenType() == TokenType.REFRESH &&
                    token.getTokenFamily() != null &&
                    !token.getTokenFamily().isBlank()
            ));
        }
    }

    @Nested
    @DisplayName("reissueAccessToken - 정상 흐름")
    class ReissueSuccess {

        @Test
        @DisplayName("유효한 Refresh Token으로 재발급 성공 시 새 토큰을 발급하고 기존 토큰을 revoke 한다")
        void reissue_success_rotatesTokens() {
            String oldRefreshValue = "old-refresh-token";
            Instant expiresAt = Instant.now().plusSeconds(1209600);

            Token oldRefreshToken = Token.builder()
                    .user(createUserWithId(USER_ID))
                    .tokenValue(oldRefreshValue)
                    .tokenType(TokenType.REFRESH)
                    .expiresAt(expiresAt)
                    .revoked(false)
                    .tokenFamily(FAMILY)
                    .build();

            TokenInfo newAccess = createTokenInfo(TokenType.ACCESS, Instant.now().plusSeconds(3600));
            TokenInfo newRefresh = createTokenInfo(TokenType.REFRESH, expiresAt);

            given(jwtProvider.validationToken(oldRefreshValue)).willReturn(TokenStatus.VALID);
            given(tokenRepository.findByTokenValueAndTokenType(oldRefreshValue, TokenType.REFRESH))
                    .willReturn(Optional.of(oldRefreshToken));
            given(jwtProvider.getUser(oldRefreshValue)).willReturn(createUserWithId(USER_ID));
            given(jwtProvider.createAccessToken(USER_ID)).willReturn(newAccess);
            given(jwtProvider.createRefreshToken(USER_ID)).willReturn(newRefresh);

            TokenReissueResponse response = authService.reissueAccessToken(oldRefreshValue);

            assertThat(response.accessToken()).isEqualTo(newAccess.tokenValue());
            assertThat(response.refreshToken()).isEqualTo(newRefresh.tokenValue());
            assertThat(oldRefreshToken.isRevoked()).isTrue();

            verify(tokenRepository).save(any(Token.class)); // 새 refresh 저장
        }
    }

    @Nested
    @DisplayName("reissueAccessToken - Refresh Token 재사용 탐지")
    class ReissueReuseDetection {

        @Test
        @DisplayName("이미 revoke 된 Refresh Token으로 재발급 시도 시 TOKEN_REUSE_DETECTED 예외 발생")
        void reissue_withRevokedToken_throwsReuseDetected() {
            String stolenRefresh = "stolen-refresh";
            Token revokedToken = Token.builder()
                    .user(createUserWithId(USER_ID))
                    .tokenValue(stolenRefresh)
                    .tokenType(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(10000))
                    .revoked(true)
                    .tokenFamily(FAMILY)
                    .build();

            given(jwtProvider.validationToken(stolenRefresh)).willReturn(TokenStatus.VALID);
            given(tokenRepository.findByTokenValueAndTokenType(stolenRefresh, TokenType.REFRESH))
                    .willReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.reissueAccessToken(stolenRefresh))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(TokenErrorCode.TOKEN_REUSE_DETECTED);

            verify(tokenRepository, never()).save(any(Token.class));
        }

        @Test
        @DisplayName("재사용 탐지 시 같은 family의 모든 토큰을 revoke 한다")
        void reissue_reuseDetection_revokesEntireFamily() {
            String stolenRefresh = "stolen-refresh";

            Token revokedToken = Token.builder()
                    .user(createUserWithId(USER_ID))
                    .tokenValue(stolenRefresh)
                    .tokenType(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(10000))
                    .revoked(true)
                    .tokenFamily(FAMILY)
                    .build();

            Token anotherActiveTokenInFamily = Token.builder()
                    .user(createUserWithId(USER_ID))
                    .tokenValue("another-refresh-in-same-family")
                    .tokenType(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(10000))
                    .revoked(false)
                    .tokenFamily(FAMILY)
                    .build();

            given(jwtProvider.validationToken(stolenRefresh)).willReturn(TokenStatus.VALID);
            given(tokenRepository.findByTokenValueAndTokenType(stolenRefresh, TokenType.REFRESH))
                    .willReturn(Optional.of(revokedToken));
            given(tokenRepository.findAllByTokenFamily(FAMILY))
                    .willReturn(List.of(revokedToken, anotherActiveTokenInFamily));

            assertThatThrownBy(() -> authService.reissueAccessToken(stolenRefresh))
                    .isInstanceOf(BusinessException.class);

            // family 내 active 토큰이 revoke 되었는지 확인
            assertThat(anotherActiveTokenInFamily.isRevoked()).isTrue();
            verify(tokenRepository).findAllByTokenFamily(FAMILY);
        }

        @Test
        @DisplayName("family가 없는 레거시 토큰 재사용 시에도 TOKEN_REUSE_DETECTED를 반환한다")
        void reissue_legacyTokenWithoutFamily_stillDetectsReuse() {
            String oldToken = "legacy-refresh-without-family";

            Token legacyRevoked = Token.builder()
                    .user(createUserWithId(USER_ID))
                    .tokenValue(oldToken)
                    .tokenType(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(10000))
                    .revoked(true)
                    .tokenFamily(null) // 마이그레이션 전 데이터
                    .build();

            given(jwtProvider.validationToken(oldToken)).willReturn(TokenStatus.VALID);
            given(tokenRepository.findByTokenValueAndTokenType(oldToken, TokenType.REFRESH))
                    .willReturn(Optional.of(legacyRevoked));

            assertThatThrownBy(() -> authService.reissueAccessToken(oldToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(TokenErrorCode.TOKEN_REUSE_DETECTED);
        }
    }

    @Nested
    @DisplayName("reissueAccessToken - 에러 케이스")
    class ReissueErrorCases {

        @Test
        @DisplayName("DB에 존재하지 않는 Refresh Token이면 TOKEN_NOT_FOUND 예외")
        void reissue_tokenNotInDb_throwsNotFound() {
            String unknownToken = "unknown";

            given(jwtProvider.validationToken(unknownToken)).willReturn(TokenStatus.VALID);
            given(tokenRepository.findByTokenValueAndTokenType(unknownToken, TokenType.REFRESH))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissueAccessToken(unknownToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(TokenErrorCode.TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("만료된 Refresh Token이면 REFRESH_TOKEN_EXPIRED 예외")
        void reissue_expiredToken_throwsExpired() {
            String expired = "expired-refresh";

            given(jwtProvider.validationToken(expired)).willReturn(TokenStatus.EXPIRED);

            assertThatThrownBy(() -> authService.reissueAccessToken(expired))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(TokenErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("유효하지 않은 JWT 형식이면 INVALID_TOKEN 예외")
        void reissue_invalidJwt_throwsInvalid() {
            String invalid = "not-a-jwt";

            given(jwtProvider.validationToken(invalid)).willReturn(TokenStatus.INVALID);

            assertThatThrownBy(() -> authService.reissueAccessToken(invalid))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(TokenErrorCode.INVALID_TOKEN);
        }
    }
}
