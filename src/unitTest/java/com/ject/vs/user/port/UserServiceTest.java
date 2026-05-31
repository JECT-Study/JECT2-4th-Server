package com.ject.vs.user.port;

import com.ject.vs.auth.domain.TokenRepository;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.adapter.web.dto.UserDeleteReq;
import com.ject.vs.user.adapter.web.dto.UserExtraInfo;
import com.ject.vs.user.adapter.web.dto.UserProfileResponse;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserDelete;
import com.ject.vs.user.domain.UserDeleteRepository;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.domain.UserStatus;
import com.ject.vs.user.exception.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WordService wordService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserDeleteRepository userDeleteRepository;

    @Mock
    private UserImageService userImageService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("추가 정보 설정 - 성공")
    void setupAdditionalInfo_Success() {
        Long userId = 1L;
        User user = User.createWithEmail("hong1234@naver.com");

        UserExtraInfo extraInfo = new UserExtraInfo(Year.of(2001), Gender.MALE, "홍길동", ImageColor.GREEN);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        UserProfileResponse result = userService.setupAdditionalInfo(extraInfo, userId);

        assertThat(result.nickname()).isEqualTo("홍길동");
        assertThat(result.birthDate()).isEqualTo(Year.of(2001));
        assertThat(result.gender()).isEqualTo(Gender.MALE);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.REGISTER);

        verify(userRepository).findById(userId);
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("물리 삭제 없이 사용자를 익명화(soft delete)하고 토큰을 제거하며 탈퇴 사유를 저장한다")
        void softDeletesUser() {
            User user = User.createWithEmail("user@example.com");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(tokenRepository.findByUserId(user)).willReturn(List.of());

            userService.deleteAccount(1L, new UserDeleteReq("앱 오류가 잦아요", "상세 의견"));

            assertThat(user.isWithdrawn()).isTrue();
            assertThat(user.getNickname()).isEqualTo(User.WITHDRAWN_NICKNAME);
            assertThat(user.getWithdrawnAt()).isNotNull();

            verify(userRepository, never()).delete(any());
            verify(tokenRepository, times(1)).deleteAll(anyList());
            verify(userDeleteRepository, times(1)).save(any(UserDelete.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
        void throwsWhenUserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteAccount(999L, new UserDeleteReq("c", "r")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(userDeleteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findOrCreate")
    class FindOrCreate {

        private static final String EMAIL = "user@example.com";

        @Test
        @DisplayName("탈퇴 후 30일 이내 동일 이메일이면 재가입이 제한된다")
        void restrictedWithinReregisterWindow() {
            User withdrawn = User.createWithEmail(EMAIL);
            withdrawn.withdraw(Instant.now()); // 방금 탈퇴 → 30일 이내
            given(userRepository.findByEmailAndUserStatus(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(List.of(withdrawn));

            assertThatThrownBy(() -> userService.findOrCreate(EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(UserErrorCode.REREGISTRATION_RESTRICTED);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("탈퇴 후 30일이 지났고 활성 계정이 없으면 기존 정보를 복구하지 않고 새 row를 생성한다")
        void createsNewRowAfterRestrictionExpired() {
            User expiredWithdrawn = User.createWithEmail(EMAIL);
            expiredWithdrawn.withdraw(Instant.now().minus(31, ChronoUnit.DAYS)); // 31일 전 탈퇴
            given(userRepository.findByEmailAndUserStatus(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(List.of(expiredWithdrawn));
            given(userRepository.findByEmailAndUserStatusNot(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(Optional.empty());
            User newUser = User.createWithEmail(EMAIL);
            given(userRepository.save(any(User.class))).willReturn(newUser);

            User result = userService.findOrCreate(EMAIL);

            assertThat(result).isSameAs(newUser);
            // 기존 탈퇴 계정은 그대로 두고(복구하지 않고) 새 row를 만든다
            assertThat(expiredWithdrawn.isWithdrawn()).isTrue();
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("탈퇴 이력이 없고 활성 계정이 있으면 신규 저장 없이 기존 계정을 반환한다")
        void returnsActiveUserWithoutSaving() {
            User active = User.createWithEmail(EMAIL);
            given(userRepository.findByEmailAndUserStatus(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(List.of());
            given(userRepository.findByEmailAndUserStatusNot(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(Optional.of(active));

            User result = userService.findOrCreate(EMAIL);

            assertThat(result).isSameAs(active);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("탈퇴 이력도 활성 계정도 없으면 신규 사용자를 생성한다")
        void createsNewUserWhenNoneExists() {
            given(userRepository.findByEmailAndUserStatus(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(List.of());
            given(userRepository.findByEmailAndUserStatusNot(EMAIL, UserStatus.WITHDRAWN))
                    .willReturn(Optional.empty());
            User newUser = User.createWithEmail(EMAIL);
            given(userRepository.save(any(User.class))).willReturn(newUser);

            User result = userService.findOrCreate(EMAIL);

            assertThat(result).isSameAs(newUser);
            verify(userRepository, times(1)).save(any(User.class));
        }
    }
}
