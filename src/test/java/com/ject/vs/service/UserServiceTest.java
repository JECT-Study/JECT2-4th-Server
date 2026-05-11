package com.ject.vs.service;

import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;
import com.ject.vs.domain.User;
import com.ject.vs.domain.UserStatus;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.util.JwtProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("추가 정보 설정 - 성공")
    void setupAdditionalInfo_Success() {
        String token = "mock-token";
        String email = "hong1234@naver.com";
        User user = User.createWithEmail(email);

        UserExtraInfo extraInfo = new UserExtraInfo(Year.of(2001), Gender.MALE, "홍길동", ImageColor.GREEN);

        given(jwtProvider.getUser(token)).willReturn(user);

        UserProfileResponse result = userService.setupAdditionalInfo(extraInfo, token);

        assertThat(result.nickname()).isEqualTo("홍길동");
        assertThat(result.birthDate()).isEqualTo(Year.of(2001));
        assertThat(result.gender()).isEqualTo(Gender.MALE);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.REGISTER);

        verify(jwtProvider).getUser(token);

//        verify(userRepository).findBySub(sub);
    }
}
