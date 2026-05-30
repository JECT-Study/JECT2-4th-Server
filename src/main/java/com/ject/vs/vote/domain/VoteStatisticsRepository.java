package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteStatisticsRepository extends JpaRepository<VoteStatistics, Long> {

    List<VoteStatistics> findAllByVoteIdIn(List<Long> voteIds);

    Optional<VoteStatistics> findByVoteId(Long voteId);
}
