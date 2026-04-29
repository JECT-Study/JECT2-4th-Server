package com.ject.vs.vote.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteParticipationTest {

    @Nested
    class of {

        @Test
        void voteId와_userId가_올바르게_설정된다() {
            // given
            Long voteId = 1L;
            Long userId = 2L;

            // when
            VoteParticipation voteParticipation = VoteParticipation.of(voteId, userId);

            // then
            assertThat(voteParticipation.getVoteId()).isEqualTo(voteId);
            assertThat(voteParticipation.getUserId()).isEqualTo(userId);
        }
    }
}
