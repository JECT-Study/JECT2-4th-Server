package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.ImageRequiredException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoteTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Nested
    class create {

        @Test
        void 일반형_투표_정상_생성() {
            Vote vote = Vote.create(
                    VoteType.GENERAL, "제목", "내용",
                    "https://thumb.url", null,
                    Duration.ofHours(24), FIXED_CLOCK
            );

            assertThat(vote.getType()).isEqualTo(VoteType.GENERAL);
            assertThat(vote.getTitle()).isEqualTo("제목");
            assertThat(vote.getStatus()).isEqualTo(VoteStatus.ONGOING);
            assertThat(vote.getEndAt()).isEqualTo(Instant.parse("2025-01-02T00:00:00Z"));
        }

        @Test
        void 몰입형_투표_imageUrl_없으면_ImageRequiredException() {
            assertThatThrownBy(() -> Vote.create(
                    VoteType.IMMERSIVE, "제목", "내용",
                    "https://thumb.url", null,
                    Duration.ofHours(24), FIXED_CLOCK
            )).isInstanceOf(ImageRequiredException.class);
        }

        @Test
        void 몰입형_투표_imageUrl_빈문자열이면_ImageRequiredException() {
            assertThatThrownBy(() -> Vote.create(
                    VoteType.IMMERSIVE, "제목", "내용",
                    "https://thumb.url", "   ",
                    Duration.ofHours(24), FIXED_CLOCK
            )).isInstanceOf(ImageRequiredException.class);
        }

        @Test
        void 몰입형_투표_imageUrl_있으면_정상_생성() {
            Vote vote = Vote.create(
                    VoteType.IMMERSIVE, "제목", "내용",
                    "https://thumb.url", "https://image.url",
                    Duration.ofHours(24), FIXED_CLOCK
            );

            assertThat(vote.getType()).isEqualTo(VoteType.IMMERSIVE);
            assertThat(vote.getImageUrl()).isEqualTo("https://image.url");
        }
    }

    @Nested
    class isOngoing {

        @Test
        void endAt_이전이면_true() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            Clock beforeEnd = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
            assertThat(vote.isOngoing(beforeEnd)).isTrue();
        }

        @Test
        void endAt_이후이면_false() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            Clock afterEnd = Clock.fixed(Instant.parse("2025-01-03T00:00:00Z"), ZoneOffset.UTC);
            assertThat(vote.isOngoing(afterEnd)).isFalse();
        }
    }

    @Nested
    class isEnded {

        @Test
        void endAt_이후이면_true() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            Clock afterEnd = Clock.fixed(Instant.parse("2025-01-03T00:00:00Z"), ZoneOffset.UTC);
            assertThat(vote.isEnded(afterEnd)).isTrue();
        }
    }

    @Nested
    class markEnded {

        @Test
        void endAt_이후에는_status가_ENDED() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            Clock afterEnd = Clock.fixed(Instant.parse("2025-01-03T00:00:00Z"), ZoneOffset.UTC);
            assertThat(vote.getStatus(afterEnd)).isEqualTo(VoteStatus.ENDED);
        }
    }

    @Nested
    class cacheAiInsight {

        @Test
        void headline과_body가_저장되고_hasAiInsight가_true() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            vote.cacheAiInsight("헤드라인", "바디");

            assertThat(vote.hasAiInsight()).isTrue();
            assertThat(vote.getAiInsightHeadline()).isEqualTo("헤드라인");
            assertThat(vote.getAiInsightBody()).isEqualTo("바디");
        }

        @Test
        void 캐시_없으면_hasAiInsight가_false() {
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            assertThat(vote.hasAiInsight()).isFalse();
        }
    }
}
