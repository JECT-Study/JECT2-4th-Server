package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {
    boolean existsByVoteIdAndUserId(Long voteId, Long userId);
    List<VoteParticipation> findAllByUserId(Long userId);
    List<VoteParticipation> findAllByVoteId(Long voteId);
    long countByVoteId(Long voteId);
}
