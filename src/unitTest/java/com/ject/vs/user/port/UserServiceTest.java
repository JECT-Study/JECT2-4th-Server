package com.ject.vs.user.port;

import com.ject.vs.user.adapter.web.dto.UserExtraInfo;
import com.ject.vs.user.adapter.web.dto.UserProfileResponse;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private WordService wordService;

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
}
