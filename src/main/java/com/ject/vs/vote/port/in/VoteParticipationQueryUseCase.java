package com.ject.vs.vote.port.in;

import java.util.List;

public interface VoteParticipationQueryUseCase {
    List<Long> findVoteIdsByUserId(Long userId);
    long countParticipantsByVoteId(Long voteId);
    List<Long> findUserIdsByVoteId(Long voteId);
}
