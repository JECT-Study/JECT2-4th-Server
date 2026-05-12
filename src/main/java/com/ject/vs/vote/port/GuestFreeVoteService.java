package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.GuestFreeVote;
import com.ject.vs.vote.domain.GuestFreeVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestFreeVoteService {

    private final GuestFreeVoteRepository repository;
    private final Clock clock;

    public void consume(String anonymousId) {
        GuestFreeVote g = repository.findById(anonymousId)
                .orElseGet(() -> GuestFreeVote.create(anonymousId));
        g.consume(clock);
        repository.save(g);
    }

    @Transactional(readOnly = true)
    public int remaining(String anonymousId) {
        return repository.findById(anonymousId)
                .map(GuestFreeVote::remaining)
                .orElse(GuestFreeVote.totalFreeVotes());
    }
}
