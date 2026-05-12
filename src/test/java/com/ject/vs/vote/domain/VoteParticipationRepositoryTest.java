package com.ject.vs.vote.domain;

import com.ject.vs.config.JpaAuditingConfig;
import com.ject.vs.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class VoteParticipationRepositoryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteOptionRepository voteOptionRepository;

    @Autowired
    private VoteParticipationRepository voteParticipationRepository;

    private Vote vote;
    private VoteOption optionA;

    @BeforeEach
    void setUp() {
        vote = voteRepository.save(
                Vote.create(VoteType.GENERAL, "테스트 투표", null, "thumb", null,
                        Duration.ofHours(24), FIXED_CLOCK)
        );
        optionA = voteOptionRepository.save(VoteOption.of(vote.getId(), "A", 0));
        voteOptionRepository.save(VoteOption.of(vote.getId(), "B", 1));
    }

    @Nested
    class existsByVoteIdAndUserId {

        @Test
        void 저장된_회원_참여자는_true를_반환한다() {
            User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
            voteParticipationRepository.save(
                    VoteParticipation.ofMember(vote.getId(), user.getId(), optionA.getId())
            );

            boolean result = voteParticipationRepository.existsByVoteIdAndUserId(vote.getId(), user.getId());

            assertThat(result).isTrue();
        }

        @Test
        void 존재하지_않는_경우_false를_반환한다() {
            boolean result = voteParticipationRepository.existsByVoteIdAndUserId(999L, 999L);
            assertThat(result).isFalse();
        }
    }

    @Nested
    class 회원_비회원_분기 {

        @Test
        void 회원_참여_저장_및_조회() {
            User user = entityManager.persistAndFlush(User.createWithSub("member-sub"));
            voteParticipationRepository.save(
                    VoteParticipation.ofMember(vote.getId(), user.getId(), optionA.getId())
            );

            var found = voteParticipationRepository.findByVoteIdAndUserId(vote.getId(), user.getId());
            assertThat(found).isPresent();
            assertThat(found.get().isGuest()).isFalse();
        }

        @Test
        void 비회원_참여_저장_및_조회() {
            String anonId = "anon-uuid-1234";
            voteParticipationRepository.save(
                    VoteParticipation.ofGuest(vote.getId(), anonId, optionA.getId())
            );

            var found = voteParticipationRepository.findByVoteIdAndAnonymousId(vote.getId(), anonId);
            assertThat(found).isPresent();
            assertThat(found.get().isGuest()).isTrue();
        }
    }

    @Nested
    class countByVoteId {

        @Test
        void 참여자_수를_올바르게_반환한다() {
            User user1 = entityManager.persistAndFlush(User.createWithSub("sub-1"));
            User user2 = entityManager.persistAndFlush(User.createWithSub("sub-2"));
            voteParticipationRepository.save(VoteParticipation.ofMember(vote.getId(), user1.getId(), optionA.getId()));
            voteParticipationRepository.save(VoteParticipation.ofMember(vote.getId(), user2.getId(), optionA.getId()));

            long count = voteParticipationRepository.countByVoteId(vote.getId());

            assertThat(count).isEqualTo(2);
        }
    }
}
