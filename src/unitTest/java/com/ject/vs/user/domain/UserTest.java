package com.ject.vs.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("탈퇴 시 상태를 WITHDRAWN으로 바꾸고 닉네임을 '알 수 없음'으로 익명화한다")
        void anonymizesUserOnWithdraw() {
            User user = User.createWithEmail("user@example.com");
            Instant now = Instant.parse("2026-01-01T00:00:00Z");

            user.withdraw(now);

            assertThat(user.isWithdrawn()).isTrue();
            assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(user.getNickname()).isEqualTo(User.WITHDRAWN_NICKNAME);
            assertThat(user.getNickname()).isEqualTo("알 수 없음");
            assertThat(user.getWithdrawnAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("탈퇴 후에도 이메일은 새 계정 재가입 처리를 위해 유지된다")
        void keepsEmailOnWithdraw() {
            User user = User.createWithEmail("user@example.com");

            user.withdraw(Instant.parse("2026-01-01T00:00:00Z"));

            assertThat(user.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("탈퇴 시 프로필 정보(생년/성별/이미지색)는 초기화된다")
        void clearsProfileOnWithdraw() {
            User user = User.createWithEmail("user@example.com");

            user.withdraw(Instant.parse("2026-01-01T00:00:00Z"));

            assertThat(user.getBirthYear()).isNull();
            assertThat(user.getGender()).isNull();
            assertThat(user.getImageColor()).isNull();
        }
    }

}
