package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestFreeVoteTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Nested
    class consume {

        @Test
        void 최초_생성_후_다섯번까지_소비할_수_있다() {
            GuestFreeVote g = GuestFreeVote.create("anon-1");

            for (int i = 0; i < 5; i++) {
                g.consume(FIXED_CLOCK);
            }

            assertThat(g.getConsumedCount()).isEqualTo(5);
            assertThat(g.remaining()).isEqualTo(0);
        }

        @Test
        void 여섯번째_소비시_VoteFreeLimitExceededException을_던진다() {
            GuestFreeVote g = GuestFreeVote.create("anon-2");
            for (int i = 0; i < 5; i++) {
                g.consume(FIXED_CLOCK);
            }

            assertThatThrownBy(() -> g.consume(FIXED_CLOCK))
                    .isInstanceOf(VoteFreeLimitExceededException.class);
        }

        @Test
        void 소비_후_lastConsumedAt이_설정된다() {
            GuestFreeVote g = GuestFreeVote.create("anon-3");

            g.consume(FIXED_CLOCK);

            assertThat(g.getLastConsumedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        }
    }

    @Nested
    class remaining {

        @Test
        void 신규_생성_시_잔여_횟수는_5이다() {
            GuestFreeVote g = GuestFreeVote.create("anon-4");
            assertThat(g.remaining()).isEqualTo(5);
        }

        @Test
        void 두번_소비_후_잔여는_3이다() {
            GuestFreeVote g = GuestFreeVote.create("anon-5");
            g.consume(FIXED_CLOCK);
            g.consume(FIXED_CLOCK);
            assertThat(g.remaining()).isEqualTo(3);
        }
    }
}
