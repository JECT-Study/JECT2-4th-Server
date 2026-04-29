package com.ject.vs.vote.port;

import com.ject.vs.vote.port.in.VoteQueryUseCase;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteQueryService implements VoteQueryUseCase {

    @Override
    public List<Long> filterVoteIdsByStatus(List<Long> voteIds, VoteStatus status) {
        // TODO: Vote 도메인 연동 후 실제 status 기반 필터링으로 교체
        return voteIds;
    }
}
