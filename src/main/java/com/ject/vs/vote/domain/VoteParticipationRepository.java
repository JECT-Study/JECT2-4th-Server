package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {

    // 기존 시그니처 유지 (채팅 도메인 및 레거시 호환)
    boolean existsByVoteIdAndUserId(Long voteId, Long userId);

    long countByVoteId(Long voteId);

    @Query("SELECT p.voteId FROM VoteParticipation p WHERE p.userId = :userId")
    List<Long> findAllVoteIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.userId FROM VoteParticipation p WHERE p.voteId = :voteId AND p.userId IS NOT NULL")
    List<Long> findAllUserIdsByVoteId(@Param("voteId") Long voteId);

    @Query("SELECT p.userId FROM VoteParticipation p WHERE p.voteId = :voteId AND p.optionId = :optionId AND p.userId IS NOT NULL")
    List<Long> findUserIdsByVoteIdAndOptionId(@Param("voteId") Long voteId, @Param("optionId") Long optionId);

    Optional<VoteParticipation> findByVoteIdAndUserId(Long voteId, Long userId);

    Optional<VoteParticipation> findByVoteIdAndAnonymousId(Long voteId, String anonymousId);

    long countByVoteIdAndOptionId(Long voteId, Long optionId);

    void deleteByVoteIdAndUserId(Long voteId, Long userId);

    @Query("""
            SELECT new com.ject.vs.vote.domain.GenderCount(u.gender, COUNT(p))
            FROM VoteParticipation p, com.ject.vs.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND p.optionId = :optionId
              AND p.userId IS NOT NULL
            GROUP BY u.gender
            """)
    List<GenderCount> findGenderDistribution(@Param("voteId") Long voteId, @Param("optionId") Long optionId);

    @Query("""
            SELECT new com.ject.vs.vote.domain.GenderCount(u.gender, COUNT(p))
            FROM VoteParticipation p, com.ject.vs.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND p.userId IS NOT NULL
            GROUP BY u.gender
            """)
    List<GenderCount> findGenderDistributionByVote(@Param("voteId") Long voteId);
}
