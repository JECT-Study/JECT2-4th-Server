package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteDetailQueryService {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteEmojiReactionRepository emojiReactionRepository;
    private final Clock clock;

    public VoteDetailResult getDetail(Long voteId, Long userId, String anonymousId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        VoteStatus status = vote.isOngoing(clock) ? VoteStatus.ONGOING : VoteStatus.ENDED;

        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        List<OptionResult> optionResults = options.stream().map(opt -> {
            long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
            int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new OptionResult(opt.getId(), opt.getLabel(), count, ratio);
        }).toList();

        Long mySelectedOptionId = null;
        if (userId != null) {
            mySelectedOptionId = voteParticipationRepository
                    .findByVoteIdAndUserId(voteId, userId)
                    .map(VoteParticipation::getOptionId)
                    .orElse(null);
        } else if (anonymousId != null) {
            mySelectedOptionId = voteParticipationRepository
                    .findByVoteIdAndAnonymousId(voteId, anonymousId)
                    .map(VoteParticipation::getOptionId)
                    .orElse(null);
        }

        Map<VoteEmoji, Long> emojiSummary = Arrays.stream(VoteEmoji.values())
                .collect(Collectors.toMap(e -> e, e -> 0L));
        emojiReactionRepository.countByEmojiForVote(voteId)
                .forEach(row -> emojiSummary.put(row.emoij(), row.count()));

        VoteEmoji myEmoji = null;
        if (userId != null) {
            myEmoji = emojiReactionRepository.findByVoteIdAndUserId(voteId, userId)
                    .map(VoteEmojiReaction::getEmoji)
                    .orElse(null);
        } else if (anonymousId != null) {
            myEmoji = emojiReactionRepository.findByVoteIdAndAnonymousId(voteId, anonymousId)
                    .map(VoteEmojiReaction::getEmoji)
                    .orElse(null);
        }

        return new VoteDetailResult(
                vote.getId(), vote.getType(), vote.getTitle(), vote.getContent(),
                vote.getThumbnailUrl(), vote.getImageUrl(), status, vote.getEndAt(),
                (int) total, optionResults, mySelectedOptionId, emojiSummary, myEmoji
        );
    }

    public record VoteDetailResult(
            Long voteId,
            VoteType type,
            String title,
            String content,
            String thumbnailUrl,
            String imageUrl,
            VoteStatus status,
            Instant endAt,
            int participantCount,
            List<OptionResult> options,
            Long mySelectedOptionId,
            Map<VoteEmoji, Long> emojiSummary,
            VoteEmoji myEmoji
    ) {
    }
}
