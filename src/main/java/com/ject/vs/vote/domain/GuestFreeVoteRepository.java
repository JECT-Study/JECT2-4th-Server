package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestFreeVoteRepository extends JpaRepository<GuestFreeVote, String> {
}
