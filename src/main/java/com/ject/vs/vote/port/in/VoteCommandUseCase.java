package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteDuration;
import com.ject.vs.vote.domain.VoteStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public interface VoteCommandUseCase {

    VoteCreateResult create(VoteCreateCommand command);

    VoteCreateResult createWithImages(VoteCreateWithImagesCommand command);

    ParticipateResult participateAsMember(Long voteId, Long userId, Long optionId);

    ParticipateResult participateAsGuest(Long voteId, String anonymousId, Long optionId);

    void cancel(Long voteId, Long userId);

    record VoteCreateCommand(
            String title,
            String content,
            String thumbnailUrl,
            String imageUrl,
            VoteDuration duration,
            String optionA,
            String optionB
    ) {
    }

    record VoteCreateWithImagesCommand(
            String title,
            String content,
            MultipartFile thumbnailFile,
            MultipartFile imageFile,
            VoteDuration duration,
            String optionA,
            String optionB
    ) {
    }

    record VoteCreateResult(Long voteId, VoteStatus status, Instant endAt) {
        public static VoteCreateResult from(Vote vote, Clock clock) {
            return new VoteCreateResult(vote.getId(), vote.getStatus(clock), vote.getEndAt());
        }
    }

    record ParticipateResult(
            Long voteId,
            Long selectedOptionId,
            List<OptionResult> options,
            int participantCount,
            Integer remainingFreeVotes
    ) {
    }

    record OptionResult(Long optionId, String label, Long voteCount, Integer ratio) {
    }
}
