package com.ject.vs.support;

import com.ject.vs.vote.domain.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

/**
 * Vote 도메인 관련 Integration Test에서 공통으로 사용하는 테스트 데이터 생성 지원 클래스.
 * BaseIntegrationTest를 상속받아 실제 PostgreSQL(Testcontainers) + 시간 제어 기능을 제공한다.
 */
public abstract class VoteIntegrationTestSupport extends BaseIntegrationTest {

    @Autowired
    protected VoteRepository voteRepository;

    @Autowired
    protected VoteStatisticsRepository voteStatisticsRepository;

    protected Vote createOngoingVote(String title) {
        Vote vote = Vote.create(
                title,
                "content for " + title,
                "https://example.com/thumb.jpg",
                null,
                Duration.ofHours(24),
                clock
        );
        return voteRepository.save(vote);
    }

    protected Vote createOngoingVoteWithEndAt(String title, Instant endAt) {
        Vote vote = createOngoingVote(title);
        entityManager.createQuery("UPDATE Vote v SET v.endAt = :endAt WHERE v.id = :id")
                .setParameter("endAt", endAt)
                .setParameter("id", vote.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return voteRepository.findById(vote.getId()).orElseThrow();
    }

    /**
     * Controller 테스트에서 사용하던 메서드명과의 호환을 위해 제공.
     * 내부적으로 createOngoingVoteWithEndAt을 호출한다.
     */
    protected Vote createOngoingVoteWithCustomEndAt(String title, Instant customEndAt) {
        return createOngoingVoteWithEndAt(title, customEndAt);
    }

    protected Vote createEndedVote(String title) {
        Vote vote = createOngoingVote(title);
        Instant past = FIXED_NOW.minus(Duration.ofDays(2));
        entityManager.createQuery("UPDATE Vote v SET v.endAt = :past WHERE v.id = :id")
                .setParameter("past", past)
                .setParameter("id", vote.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return voteRepository.findById(vote.getId()).orElseThrow();
    }

    protected void saveViewCount(Long voteId, long viewCount) {
        entityManager.createNativeQuery(
                        "INSERT INTO vote_statistics (vote_id, view_count) VALUES (:voteId, :count)")
                .setParameter("voteId", voteId)
                .setParameter("count", viewCount)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
