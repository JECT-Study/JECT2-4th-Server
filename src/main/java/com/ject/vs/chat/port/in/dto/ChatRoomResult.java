package com.ject.vs.chat.port.in.dto;

import com.ject.vs.vote.domain.VoteOptionCode;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.port.in.VoteQueryUseCase.VoteChatSummary;

import java.time.Instant;

public record ChatRoomResult(
        Long voteId,
        String title,
        VoteStatus status,
        int participantCount,
        String optionA,
        String optionB,
        Instant endAt,
        VoteOptionCode myVoteOption
) {
    public static ChatRoomResult of(VoteChatSummary vote, int participantCount, VoteOptionCode myVoteOption) {
        return new ChatRoomResult(
                vote.voteId(),
                vote.title(),
                vote.status(),
                participantCount,
                vote.optionA(),
                vote.optionB(),
                vote.endAt(),
                myVoteOption
        );
    }
}
