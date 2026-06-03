package com.ject.vs.vote.port;

import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImmersiveVoteQueryService implements ImmersiveVoteQueryUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteEmojiReactionRepository emojiReactionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;

    @Override
    public ImmersiveFeedResult getFeed(Long cursor, Long startVoteId, int size, Long userId, String anonymousId) {
        Instant now = Instant.now(clock);

        List<ImmersiveFeedItem> items;
        boolean hasNext;

        if (cursor != null) {
            PageRequest pageable = PageRequest.of(0, size);
            Slice<Vote> slice = voteRepository.findByIdLessThanAndEndAtAfterOrderByIdDesc(
                    cursor, now, pageable);
            items = slice.getContent().stream().map(v -> toFeedItem(v, userId, anonymousId)).toList();
            hasNext = slice.hasNext();
        } else if (startVoteId != null) {
            Vote startVote = voteRepository.findById(startVoteId).orElse(null);
            if (startVote != null && !startVote.isEnded(clock)) {
                Slice<Vote> rest = voteRepository.findByIdLessThanAndEndAtAfterOrderByIdDesc(
                        startVoteId, now, PageRequest.of(0, size - 1));
                List<ImmersiveFeedItem> combined = new ArrayList<>();
                combined.add(toFeedItem(startVote, userId, anonymousId));
                rest.getContent().stream().map(v -> toFeedItem(v, userId, anonymousId)).forEach(combined::add);
                items = combined;
                hasNext = rest.hasNext();
            } else {
                Slice<Vote> slice = voteRepository.findByEndAtAfterOrderByIdDesc(
                        now, PageRequest.of(0, size));
                items = slice.getContent().stream().map(v -> toFeedItem(v, userId, anonymousId)).toList();
                hasNext = slice.hasNext();
            }
        } else {
            Slice<Vote> slice = voteRepository.findByEndAtAfterOrderByIdDesc(
                    now, PageRequest.of(0, size));
            items = slice.getContent().stream().map(v -> toFeedItem(v, userId, anonymousId)).toList();
            hasNext = slice.hasNext();
        }

        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).voteId() : null;
        return new ImmersiveFeedResult(items, nextCursor, hasNext);
    }

    @Override
    public ImmersiveLiveResult getLive(Long voteId) {
        if (!voteRepository.existsById(voteId)) {
            throw new VoteNotFoundException();
        }

        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        List<LiveOptionItem> liveOptions = options.stream().map(opt -> {
            long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
            int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new LiveOptionItem(opt.getId(), count, ratio);
        }).toList();

        // TODO: currentViewerCount — Redis 도입 후 갱신 예정
        return new ImmersiveLiveResult(liveOptions, 0, (int) total);
    }

    @Override
    public ImmersiveNextResult getNextRandom(List<Long> excludeIds, int size, Long userId, String anonymousId) {
        Instant now = Instant.now(clock);
        PageRequest pageable = PageRequest.of(0, size);

        Slice<Vote> slice;
        if (excludeIds == null || excludeIds.isEmpty()) {
            slice = voteRepository.findRandom(now, pageable);
        } else {
            slice = voteRepository.findRandomExcluding(now, excludeIds, pageable);
        }

        List<ImmersiveFeedItem> items = slice.getContent().stream()
                .map(v -> toFeedItem(v, userId, anonymousId))
                .toList();

        return new ImmersiveNextResult(items);
    }

    private ImmersiveFeedItem toFeedItem(Vote vote, Long userId, String anonymousId) {
        Long voteId = vote.getId();
        long total = voteParticipationRepository.countByVoteId(voteId);

        // 내 투표 정보
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
        boolean voted = mySelectedOptionId != null;

        // 옵션 정보 (투표 전이면 voteCount/ratio null)
        List<VoteOption> voteOptions = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        List<FeedOptionItem> options = voteOptions.stream().map(opt -> {
            if (voted) {
                long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
                int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                return new FeedOptionItem(opt.getId(), opt.getLabel(), count, ratio);
            } else {
                return new FeedOptionItem(opt.getId(), opt.getLabel(), null, null);
            }
        }).toList();

        // 이모지 정보
        Map<VoteEmoji, Long> emojiSummary = Arrays.stream(VoteEmoji.values())
                .collect(Collectors.toMap(e -> e, e -> 0L));
        emojiReactionRepository.countByEmojiForVote(voteId)
                .forEach(row -> emojiSummary.put(row.emoij(), row.count()));
        long emojiTotal = emojiSummary.values().stream().mapToLong(Long::longValue).sum();

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

        int commentCount = (int) chatMessageRepository.countByVoteId(voteId);

        // imageFile 없이 생성된 투표는 imageUrl이 null이므로 thumbnailUrl로 폴백한다.
        String imageUrl = vote.getImageUrl() != null ? vote.getImageUrl() : vote.getThumbnailUrl();

        return new ImmersiveFeedItem(
                voteId,
                vote.getTitle(),
                vote.getContent(),
                imageUrl,
                vote.getEndAt(),
                options,
                voted,
                mySelectedOptionId,
                emojiSummary,
                emojiTotal,
                myEmoji,
                commentCount,
                0  // TODO: Redis viewer count
        );
    }
}
