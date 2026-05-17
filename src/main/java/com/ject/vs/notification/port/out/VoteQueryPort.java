package com.ject.vs.notification.port.out;

import com.ject.vs.vote.domain.Vote;

public interface VoteQueryPort {

    /**
     * Vote 조회. 존재하지 않으면 VoteNotFoundException 발생.
     */
    Vote getById(Long voteId);
}
