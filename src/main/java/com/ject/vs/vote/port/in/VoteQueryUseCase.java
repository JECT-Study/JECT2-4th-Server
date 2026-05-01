package com.ject.vs.vote.port.in;

import com.ject.vs.vote.port.in.dto.VoteStatus;

import java.util.List;

public interface VoteQueryUseCase {
    // TODO: Vote 도메인 연동 후 실제 status 기반 필터링으로 교체
    List<Long> findAllVoteIdsByStatus(List<Long> voteIds, VoteStatus status);
}
