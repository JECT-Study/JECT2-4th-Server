package com.ject.vs.vote.domain;

import com.ject.vs.domain.User;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class existsByVoteIdAndUserId {

        @Test
        void 저장된_참여자는_true를_반환한다() {
            // given
            User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
            Vote vote = voteRepository.save(Vote.of());
            voteParticipationRepository.save(VoteParticipation.of(vote.getId(), user.getId()));

            // when
            boolean result = voteParticipationRepository.existsByVoteIdAndUserId(vote.getId(), user.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        void 존재하지_않는_경우_false를_반환한다() {
            // given
            // (no data)

            // when
            boolean result = voteParticipationRepository.existsByVoteIdAndUserId(999L, 999L);

            // then
            assertThat(result).isFalse();
        }
    }
}
