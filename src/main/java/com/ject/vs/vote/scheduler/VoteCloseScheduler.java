package com.ject.vs.vote.scheduler;

import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteCloseScheduler {

    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredVotes() {
        List<Vote> expired = voteRepository.findExpiredOngoing(Instant.now(clock));
        for (Vote vote : expired) {
            eventPublisher.publishEvent(new VoteEndedEvent(vote.getId()));
        }
        if (!expired.isEmpty()) {
            log.info("Closed {} expired votes", expired.size());
        }
    }
}
