package com.ject.vs.user.port;

import com.ject.vs.image.port.ImageService;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @MockitoBean
    private ImageService imageService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findOrCreate는 신규 사용자를 저장하고 같은 이메일이면 기존 사용자를 반환한다")
    void findOrCreate_persistsNewUserAndReturnsExistingUser() {
        String email = "integration-test@example.com";

        User created = userService.findOrCreate(email);
        User found = userService.findOrCreate(email);

        assertThat(created.getId()).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo(email);
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("findOrCreate는 탈퇴한 동일 이메일 사용자를 복구하지 않고 새 사용자를 생성한다")
    void findOrCreate_createsNewUserWhenSameEmailUserIsWithdrawn() {
        String email = "withdrawn-integration-test@example.com";
        User withdrawn = userService.findOrCreate(email);
        withdrawn.withdraw(Instant.parse("2026-01-01T00:00:00Z"));
        userRepository.flush();

        User rejoined = userService.findOrCreate(email);

        assertThat(rejoined.getId()).isNotEqualTo(withdrawn.getId());
        assertThat(rejoined.getEmail()).isEqualTo(email);
        assertThat(rejoined.isWithdrawn()).isFalse();
        assertThat(withdrawn.isWithdrawn()).isTrue();
        assertThat(userRepository.findAll()).hasSize(2);
    }
}
