package com.ject.vs.vote.scheduler;

import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
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
            vote.markEnded();
            eventPublisher.publishEvent(new VoteEndedEvent(vote.getId()));
        }
        if (!expired.isEmpty()) {
            log.info("Closed {} expired votes", expired.size());
        }
    }

    /**
     * 서버 재시작 시 스케줄러가 돌기 전에 이미 만료된 투표를 보정한다.
     * @PostConstruct는 ApplicationContext 초기화를 블록하므로
     * ApplicationReadyEvent + @Async로 부팅 완료 후 백그라운드에서 실행.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("voteCloseExecutor")
    public void closeExpiredOnStartup() {
        log.info("Running startup vote close compensation");
        closeExpiredVotes();
    }
}
