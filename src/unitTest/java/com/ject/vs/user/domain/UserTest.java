package com.ject.vs.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
        @DisplayName("탈퇴 후에도 이메일은 재가입 제한 판정을 위해 유지된다")
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

    @Nested
    @DisplayName("isReregisterRestricted")
    class IsReregisterRestricted {

        @Test
        @DisplayName("탈퇴 이력이 없으면(미탈퇴) 제한되지 않는다")
        void notRestrictedWhenNotWithdrawn() {
            User user = User.createWithEmail("user@example.com");

            assertThat(user.isReregisterRestricted(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("탈퇴 후 30일 이내면 재가입이 제한된다")
        void restrictedWithin30Days() {
            User user = User.createWithEmail("user@example.com");
            Instant withdrawnAt = Instant.parse("2026-01-01T00:00:00Z");
            user.withdraw(withdrawnAt);

            Instant day29 = withdrawnAt.plus(29, ChronoUnit.DAYS);

            assertThat(user.isReregisterRestricted(day29)).isTrue();
        }

        @Test
        @DisplayName("탈퇴 후 정확히 30일이 지나면 제한되지 않는다")
        void notRestrictedAtExactly30Days() {
            User user = User.createWithEmail("user@example.com");
            Instant withdrawnAt = Instant.parse("2026-01-01T00:00:00Z");
            user.withdraw(withdrawnAt);

            Instant day30 = withdrawnAt.plus(30, ChronoUnit.DAYS);

            assertThat(user.isReregisterRestricted(day30)).isFalse();
        }

        @Test
        @DisplayName("탈퇴 후 30일이 지나면 재가입이 제한되지 않는다")
        void notRestrictedAfter30Days() {
            User user = User.createWithEmail("user@example.com");
            Instant withdrawnAt = Instant.parse("2026-01-01T00:00:00Z");
            user.withdraw(withdrawnAt);

            Instant day31 = withdrawnAt.plus(31, ChronoUnit.DAYS);

            assertThat(user.isReregisterRestricted(day31)).isFalse();
        }
    }
}
