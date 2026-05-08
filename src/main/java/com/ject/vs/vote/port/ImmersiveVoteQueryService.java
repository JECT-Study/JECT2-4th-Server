package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImmersiveVoteQueryService implements ImmersiveVoteQueryUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final Clock clock;

    @Override
    public ImmersiveFeedResult getFeed(Long cursor, int size, Long userId, String anonymousId) {
        PageRequest pageable = PageRequest.of(0, size);

        Slice<Vote> slice = cursor == null
                ? voteRepository.findByTypeOrderByEndAtDesc(VoteType.IMMERSIVE, pageable)
                : voteRepository.findByTypeAndIdLessThanOrderByEndAtDesc(VoteType.IMMERSIVE, cursor, pageable);

        List<ImmersiveFeedItem> items = slice.getContent().stream()
                .map(vote -> toFeedItem(vote, userId, anonymousId))
                .toList();

        Long nextCursor = slice.hasNext()
                ? items.get(items.size() - 1).voteId()
                : null;

        return new ImmersiveFeedResult(items, nextCursor, slice.hasNext());
    }

    @Override
    public ImmersiveLiveResult getLive(Long voteId) {
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        int aRatio = 0;
        int bRatio = 0;
        if (!options.isEmpty() && total > 0) {
            long aCount = voteParticipationRepository.countByVoteIdAndOptionId(voteId, options.get(0).getId());
            aRatio = (int) Math.round(aCount * 100.0 / total);
            bRatio = 100 - aRatio;
        }

        // TODO: currentViewerCount — Redis 도입 후 갱신 예정
        return new ImmersiveLiveResult(voteId, aRatio, bRatio, (int) total, 0);
    }

    private ImmersiveFeedItem toFeedItem(Vote vote, Long userId, String anonymousId) {
        VoteStatus status = vote.isOngoing(clock) ? VoteStatus.ONGOING : VoteStatus.ENDED;
        int participantCount = (int) voteParticipationRepository.countByVoteId(vote.getId());

        Long mySelectedOptionId = null;
        if (userId != null) {
            mySelectedOptionId = voteParticipationRepository
                    .findByVoteIdAndUserId(vote.getId(), userId)
                    .map(VoteParticipation::getOptionId)
                    .orElse(null);
        } else if (anonymousId != null) {
            mySelectedOptionId = voteParticipationRepository
                    .findByVoteIdAndAnonymousId(vote.getId(), anonymousId)
                    .map(VoteParticipation::getOptionId)
                    .orElse(null);
        }

        return new ImmersiveFeedItem(
                vote.getId(),
                vote.getTitle(),
                vote.getImageUrl(),
                status,
                vote.getEndAt(),
                participantCount,
                0, // TODO: Redis viewer count
                mySelectedOptionId
        );
    }
}
