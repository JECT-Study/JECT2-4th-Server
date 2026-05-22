package com.ject.vs.vote.port.in;

import com.ject.vs.vote.adapter.web.dto.MyParticipatedVoteResponse;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteSortType;

import java.util.List;

public interface VoteParticipationQueryUseCase {
    boolean isParticipant(Long voteId, Long userId);
    List<Long> findAllVoteIdsByUserId(Long userId);
    long countParticipantsByVoteId(Long voteId);
    List<Long> findAllUserIdsByVoteId(Long voteId);
    MyParticipatedVoteResponse findVotesByOrder(Long userId, VoteSortType type);
    MyParticipatedVoteResponse findVotesEndByOrder(Long userId, VoteSortType type);
}
