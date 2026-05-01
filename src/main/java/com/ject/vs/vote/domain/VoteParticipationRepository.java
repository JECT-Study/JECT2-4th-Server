package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {
    boolean existsByVoteIdAndUserId(Long voteId, Long userId);
    long countByVoteId(Long voteId);

    @Query("SELECT p.voteId FROM VoteParticipation p WHERE p.userId = :userId")
    List<Long> findAllVoteIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.userId FROM VoteParticipation p WHERE p.voteId = :voteId")
    List<Long> findAllUserIdsByVoteId(@Param("voteId") Long voteId);
}
