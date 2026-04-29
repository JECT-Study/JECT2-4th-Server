package com.ject.vs.vote.domain;

import com.ject.vs.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VoteParticipationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteParticipationRepository voteParticipationRepository;

    @Test
    void 저장_후_existsByVoteIdAndUserId가_true를_반환한다() {
        User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
        Vote vote = voteRepository.save(Vote.createForTest());
        VoteParticipation voteParticipation = VoteParticipation.of(vote.getId(), user.getId());
        voteParticipationRepository.save(voteParticipation);

        boolean result = voteParticipationRepository.existsByVoteIdAndUserId(vote.getId(), user.getId());

        assertThat(result).isTrue();
    }

    @Test
    void 존재하지_않는_경우_existsByVoteIdAndUserId가_false를_반환한다() {
        boolean result = voteParticipationRepository.existsByVoteIdAndUserId(999L, 999L);

        assertThat(result).isFalse();
    }
}
