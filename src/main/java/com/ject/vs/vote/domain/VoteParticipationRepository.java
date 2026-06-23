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

    long countByUserId(Long userId);

    @Query(value = """
            SELECT vp.vote_id
            FROM vote_participation vp
            WHERE vp.user_id = :userId
            ORDER BY GREATEST(
                vp.updated_at,
                COALESCE(
                    (SELECT MAX(cm.created_at)
                     FROM chat_message cm
                     WHERE cm.sender_id = :userId AND cm.vote_id = vp.vote_id),
                    vp.created_at
                )
            ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTopVoteIdsByRecentActivity(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    @Query("""
            SELECT new com.ject.vs.vote.domain.GenderCount(u.gender, COUNT(p))
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND p.optionId = :optionId
              AND p.userId IS NOT NULL
            GROUP BY u.gender
            """)
    List<GenderCount> findGenderDistribution(@Param("voteId") Long voteId, @Param("optionId") Long optionId);

    @Query("""
            SELECT new com.ject.vs.vote.domain.GenderCount(u.gender, COUNT(p))
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND p.userId IS NOT NULL
            GROUP BY u.gender
            """)
    List<GenderCount> findGenderDistributionByVote(@Param("voteId") Long voteId);

    @Query("""
            SELECT new com.ject.vs.vote.domain.VoteParticipationRepository$VoteParticipantCount(p.voteId, COUNT(p))
            FROM VoteParticipation p
            WHERE p.voteId IN :voteIds
            GROUP BY p.voteId
            """)
    List<VoteParticipantCount> countByVoteIds(@Param("voteIds") List<Long> voteIds);

    record VoteParticipantCount(Long voteId, Long count) {
    }

    @Query("""
            SELECT COUNT(p)
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND p.optionId = :optionId
              AND u.gender = :gender
              AND p.userId IS NOT NULL
            """)
    long countByVoteIdAndOptionIdAndGender(
            @Param("voteId") Long voteId,
            @Param("optionId") Long optionId,
            @Param("gender") com.ject.vs.user.domain.Gender gender);

    @Query("""
            SELECT COUNT(p)
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND u.gender = :gender
              AND p.userId IS NOT NULL
            """)
    long countByVoteIdAndGender(
            @Param("voteId") Long voteId,
            @Param("gender") com.ject.vs.user.domain.Gender gender);

    @Query("""
            SELECT p.optionId, COUNT(p) as cnt
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND u.gender = :gender
              AND p.userId IS NOT NULL
            GROUP BY p.optionId
            ORDER BY cnt DESC
            """)
    List<Object[]> findOptionCountsByVoteIdAndGender(
            @Param("voteId") Long voteId,
            @Param("gender") com.ject.vs.user.domain.Gender gender);

    @Query("""
            SELECT p.optionId, COUNT(p) as cnt
            FROM VoteParticipation p, com.ject.vs.user.domain.User u
            WHERE p.userId = u.id
              AND p.voteId = :voteId
              AND u.birthYear IS NOT NULL
              AND p.userId IS NOT NULL
            GROUP BY p.optionId
            """)
    List<Object[]> findOptionCountsByVoteId(@Param("voteId") Long voteId);
}
