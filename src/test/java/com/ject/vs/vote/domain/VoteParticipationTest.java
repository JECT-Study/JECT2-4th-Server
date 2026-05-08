package com.ject.vs.vote.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteParticipationTest {

    @Nested
    class ofMember {

        @Test
        void 회원_참여_정보가_올바르게_설정된다() {
            VoteParticipation p = VoteParticipation.ofMember(1L, 2L, 10L);

            assertThat(p.getVoteId()).isEqualTo(1L);
            assertThat(p.getUserId()).isEqualTo(2L);
            assertThat(p.getOptionId()).isEqualTo(10L);
            assertThat(p.getAnonymousId()).isNull();
            assertThat(p.isGuest()).isFalse();
        }
    }

    @Nested
    class ofGuest {

        @Test
        void 비회원_참여_정보가_올바르게_설정된다() {
            VoteParticipation p = VoteParticipation.ofGuest(1L, "anon-uuid", 10L);

            assertThat(p.getVoteId()).isEqualTo(1L);
            assertThat(p.getAnonymousId()).isEqualTo("anon-uuid");
            assertThat(p.getOptionId()).isEqualTo(10L);
            assertThat(p.getUserId()).isNull();
            assertThat(p.isGuest()).isTrue();
        }
    }

    @Nested
    class changeOption {

        @Test
        void optionId가_변경된다() {
            VoteParticipation p = VoteParticipation.ofMember(1L, 2L, 10L);
            p.changeOption(20L);
            assertThat(p.getOptionId()).isEqualTo(20L);
        }
    }

    @Nested
    class isGuest {

        @Test
        void 회원이면_false() {
            assertThat(VoteParticipation.ofMember(1L, 2L, 10L).isGuest()).isFalse();
        }

        @Test
        void 비회원이면_true() {
            assertThat(VoteParticipation.ofGuest(1L, "uuid", 10L).isGuest()).isTrue();
        }
    }
}
