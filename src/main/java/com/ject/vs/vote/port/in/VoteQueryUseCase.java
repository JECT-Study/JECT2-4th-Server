package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VoteQueryUseCase {

    boolean isParticipated(Long voteId, Long userId);

    Optional<Long> getSelectedOptionId(Long voteId, Long userId);

    VoteSummary getVoteSummary(Long voteId);

    VoteRatio getRatio(Long voteId);

    int getParticipantCount(Long voteId);

    /** 채팅 도메인 getChatList() 호환용 — 실제 Vote.endAt 기준으로 필터링 */
    List<Long> findAllVoteIdsByStatus(List<Long> voteIds, VoteStatus status);

    record VoteSummary(Long voteId, String title, VoteStatus status, Instant endAt) {
    }

    record VoteRatio(int optionARatio, int optionBRatio, int participantCount) {
    }
}
