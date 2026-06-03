package com.ject.vs.vote.domain;

import com.ject.vs.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("Vote 저장/조회 통합 테스트")
class VoteRepositoryIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2025-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(BASE_TIME, ZoneOffset.UTC);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteOptionRepository voteOptionRepository;

    @Nested
    @DisplayName("Vote 저장 테스트")
    class VoteSaveTest {

        @Test
        @DisplayName("일반형 투표 저장 시 모든 필드가 DB에 정확히 저장된다")
        void 일반형_투표_저장_시_모든_필드가_DB에_저장된다() {
            // given
            Vote vote = Vote.create(
                    "테스트 제목",
                    "테스트 내용",
                    "https://example.com/thumb.png",
                    null,
                    Duration.ofHours(24),
                    FIXED_CLOCK
            );

            // when
            Vote savedVote = voteRepository.save(vote);
            entityManager.flush();
            entityManager.clear();

            // then
            Vote foundVote = voteRepository.findById(savedVote.getId()).orElseThrow();

            assertThat(foundVote.getId()).isNotNull();
            assertThat(foundVote.getTitle()).isEqualTo("테스트 제목");
            assertThat(foundVote.getContent()).isEqualTo("테스트 내용");
            assertThat(foundVote.getThumbnailUrl()).isEqualTo("https://example.com/thumb.png");
            assertThat(foundVote.getImageUrl()).isNull();
            assertThat(foundVote.getEndAt()).isEqualTo(BASE_TIME.plus(Duration.ofHours(24)));
            assertThat(foundVote.getCreatedAt()).isNotNull();
            assertThat(foundVote.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("몰입형 투표 저장 시 imageUrl이 함께 저장된다")
        void 몰입형_투표_저장_시_imageUrl이_함께_저장된다() {
            // given
            Vote vote = Vote.create(
                    "몰입형 제목",
                    "몰입형 내용",
                    "https://example.com/thumb.png",
                    "https://example.com/image.png",
                    Duration.ofHours(48),
                    FIXED_CLOCK
            );

            // when
            Vote savedVote = voteRepository.save(vote);
            entityManager.flush();
            entityManager.clear();

            // then
            Vote foundVote = voteRepository.findById(savedVote.getId()).orElseThrow();

            assertThat(foundVote.getImageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(foundVote.getEndAt()).isEqualTo(BASE_TIME.plus(Duration.ofHours(48)));
        }

        @Test
        @DisplayName("다양한 duration으로 투표 저장 시 endAt이 정확히 계산되어 저장된다")
        void 다양한_duration으로_투표_저장_시_endAt이_정확히_계산된다() {
            // given
            Vote vote1h = Vote.create("1시간", null, "thumb", null,
                    Duration.ofHours(1), FIXED_CLOCK);
            Vote vote12h = Vote.create("12시간", null, "thumb", null,
                    Duration.ofHours(12), FIXED_CLOCK);
            Vote vote24h = Vote.create("24시간", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);

            // when
            voteRepository.saveAll(List.of(vote1h, vote12h, vote24h));
            entityManager.flush();
            entityManager.clear();

            // then
            List<Vote> allVotes = voteRepository.findAll();

            Vote found1h = allVotes.stream().filter(v -> v.getTitle().equals("1시간")).findFirst().orElseThrow();
            Vote found12h = allVotes.stream().filter(v -> v.getTitle().equals("12시간")).findFirst().orElseThrow();
            Vote found24h = allVotes.stream().filter(v -> v.getTitle().equals("24시간")).findFirst().orElseThrow();

            assertThat(found1h.getEndAt()).isEqualTo(BASE_TIME.plus(Duration.ofHours(1)));
            assertThat(found12h.getEndAt()).isEqualTo(BASE_TIME.plus(Duration.ofHours(12)));
            assertThat(found24h.getEndAt()).isEqualTo(BASE_TIME.plus(Duration.ofHours(24)));
        }
    }

    @Nested
    @DisplayName("Vote + VoteOption 함께 저장 테스트")
    class VoteWithOptionsTest {

        @Test
        @DisplayName("투표와 옵션이 함께 저장되고 조회된다")
        void 투표와_옵션이_함께_저장되고_조회된다() {
            // given
            Vote vote = Vote.create("선택 투표", "A vs B",
                    "thumb", null, Duration.ofHours(24), FIXED_CLOCK);
            Vote savedVote = voteRepository.save(vote);

            VoteOption optionA = VoteOption.of(savedVote, "선택지 A", 0);
            VoteOption optionB = VoteOption.of(savedVote, "선택지 B", 1);
            voteOptionRepository.saveAll(List.of(optionA, optionB));

            entityManager.flush();
            entityManager.clear();

            // when
            List<VoteOption> foundOptions = voteOptionRepository.findByVoteIdOrderByPosition(savedVote.getId());

            // then
            assertThat(foundOptions).hasSize(2);
            assertThat(foundOptions.get(0).getLabel()).isEqualTo("선택지 A");
            assertThat(foundOptions.get(0).getPosition()).isEqualTo(0);
            assertThat(foundOptions.get(1).getLabel()).isEqualTo("선택지 B");
            assertThat(foundOptions.get(1).getPosition()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("진행중/종료된 투표 조회 테스트")
    class VoteStatusQueryTest {

        private Vote ongoingVote;
        private Vote expiredVote;

        @BeforeEach
        void setUp() {
            // 진행 중인 투표: endAt = BASE_TIME + 24h
            ongoingVote = voteRepository.save(
                    Vote.create("진행중 투표", null, "thumb", null,
                            Duration.ofHours(24), FIXED_CLOCK)
            );

            // 종료된 투표: endAt = BASE_TIME + 1h (조회 시점 BASE_TIME + 2h 기준으로 종료됨)
            expiredVote = voteRepository.save(
                    Vote.create("종료된 투표", null, "thumb", null,
                            Duration.ofHours(1), FIXED_CLOCK)
            );

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("findOngoingVotes는 현재 시각 기준 진행 중인 투표만 반환한다")
        void findOngoingVotes는_진행중인_투표만_반환한다() {
            // given
            Instant queryTime = BASE_TIME.plus(Duration.ofHours(2)); // 1시간짜리는 종료됨

            // when
            List<Vote> ongoingVotes = voteRepository.findOngoingVotes(queryTime);

            // then
            assertThat(ongoingVotes).hasSize(1);
            assertThat(ongoingVotes.get(0).getTitle()).isEqualTo("진행중 투표");
        }

        @Test
        @DisplayName("findExpiredOngoing은 현재 시각 기준 종료된 투표를 반환한다")
        void findExpiredOngoing은_종료된_투표를_반환한다() {
            // given
            Instant queryTime = BASE_TIME.plus(Duration.ofHours(2));

            // when
            List<Vote> expiredVotes = voteRepository.findExpiredOngoing(queryTime);

            // then
            assertThat(expiredVotes).hasSize(1);
            assertThat(expiredVotes.get(0).getTitle()).isEqualTo("종료된 투표");
        }

        @Test
        @DisplayName("getStatus는 endAt 기준으로 ONGOING/ENDED를 정확히 반환한다")
        void getStatus는_endAt_기준으로_상태를_반환한다() {
            // given
            Vote foundOngoing = voteRepository.findById(ongoingVote.getId()).orElseThrow();
            Vote foundExpired = voteRepository.findById(expiredVote.getId()).orElseThrow();

            Clock afterExpiryClock = Clock.fixed(BASE_TIME.plus(Duration.ofHours(2)), ZoneOffset.UTC);

            // when & then
            assertThat(foundOngoing.getStatus(afterExpiryClock)).isEqualTo(VoteStatus.ONGOING);
            assertThat(foundExpired.getStatus(afterExpiryClock)).isEqualTo(VoteStatus.ENDED);
        }
    }

    @Nested
    @DisplayName("AI Insight 캐싱 테스트")
    class AiInsightCacheTest {

        @Test
        @DisplayName("AI Insight가 저장되고 조회된다")
        void AI_Insight가_저장되고_조회된다() {
            // given
            Vote vote = voteRepository.save(
                    Vote.create("AI 분석 투표", null, "thumb", null,
                            Duration.ofHours(24), FIXED_CLOCK)
            );
            vote.cacheAiInsight("AI 분석 헤드라인", "AI 분석 본문 내용");
            voteRepository.save(vote);

            entityManager.flush();
            entityManager.clear();

            // when
            Vote foundVote = voteRepository.findById(vote.getId()).orElseThrow();

            // then
            assertThat(foundVote.hasAiInsight()).isTrue();
            assertThat(foundVote.getAiInsightHeadline()).isEqualTo("AI 분석 헤드라인");
            assertThat(foundVote.getAiInsightBody()).isEqualTo("AI 분석 본문 내용");
        }
    }
}
