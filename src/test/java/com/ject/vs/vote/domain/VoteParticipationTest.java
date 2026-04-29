package com.ject.vs.vote.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteParticipationTest {

    @Test
    void of_팩토리로_생성시_voteId와_userId가_올바르게_설정된다() {
        Long voteId = 1L;
        Long userId = 2L;

        VoteParticipation voteParticipation = VoteParticipation.of(voteId, userId);

        assertThat(voteParticipation.getVoteId()).isEqualTo(voteId);
        assertThat(voteParticipation.getUserId()).isEqualTo(userId);
    }
}
