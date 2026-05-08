package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.ImmersiveVoteAction;

import java.util.List;

public interface ImmersiveVoteCommandUseCase {

    /**
     * 같은 옵션 재클릭 → CANCELED, 다른 옵션 or 신규 → VOTED.
     * 회원이면 userId 사용, 비회원이면 anonymousId 사용.
     */
    ImmersiveParticipateResult participateOrCancel(
            Long voteId, Long userId, String anonymousId, Long optionId);

    record ImmersiveParticipateResult(
            Long voteId,
            ImmersiveVoteAction action,
            Long selectedOptionId,
            List<VoteCommandUseCase.OptionResult> options,
            Integer remainingFreeVotes
    ) {
    }
}
