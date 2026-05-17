package com.ject.vs.notification.adapter.out.vote;

import com.ject.vs.notification.port.out.VoteQueryPort;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.exception.VoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoteQueryAdapter implements VoteQueryPort {

    private final VoteRepository voteRepository;

    @Override
    public Vote getById(Long voteId) {
        return voteRepository.findById(voteId)
                .orElseThrow(VoteNotFoundException::new);
    }
}
