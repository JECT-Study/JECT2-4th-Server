package com.ject.vs.vote.port.in;

import java.util.List;

public interface VoteParticipationQueryUseCase {
    boolean isParticipant(Long voteId, Long userId);
    List<Long> findAllVoteIdsByUserId(Long userId);
    long countParticipantsByVoteId(Long voteId);
    List<Long> findAllUserIdsByVoteId(Long voteId);
}
