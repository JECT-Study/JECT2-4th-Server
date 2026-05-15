package com.ject.vs.vote.adapter.handler;

import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoteEventHandler {

    /**
     * VoteCloseScheduler 요기서 해당 이벤트가 발행됨\
     *
     * EventListener는 동기로 동작해 그래서 처리량을 위해 async를 사용합니다.
     *
     * 다만 다음과 같은 고민할 부분이 존재합니다.
     *
     * 1. 비동기로 처리할 때, 유실되면 어떻게 처리할거에요?
     * 2. 스케줄러에 의해 트리거 되는데, 1분 주기보다 처리 시간이 길어지면 같은 처리가 동시에 진행될 수 있어 보여요 어떻게 처리할거에요?
     * 3. 멱등하게 어떻게 처리할 수 있을까요?
     *
     *
     * 트랜잭션 아웃박스 패턴을 보면 좋을듯!
     * 분산락을 사용할 수 있어보임
     *
     * 아웃박스를 구현할 떄, 발행 기준으로 저장할래? 아니면 컨슈밍 기준으로 만들래?
     */
    @EventListener(classes = VoteEndedEvent.class)
    @Async("voteCloseExecutor")
    public void handleVoteEnded(VoteEndedEvent event){
        // log 추가하면 좋아보임
        // ai 분석을 시작하면 좋아보임
    }
}
