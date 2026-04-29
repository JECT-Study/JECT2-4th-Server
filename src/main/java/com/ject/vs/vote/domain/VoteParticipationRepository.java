package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {
    boolean existsByVoteIdAndUserId(Long voteId, Long userId);
}
