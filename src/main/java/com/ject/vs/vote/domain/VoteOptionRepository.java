package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    List<VoteOption> findByVoteIdOrderByPosition(Long voteId);

    boolean existsByIdAndVoteId(Long id, Long voteId);
}
