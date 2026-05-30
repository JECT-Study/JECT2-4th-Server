package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.domain.VoteType;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public record VoteDetailResponse(
        Long voteId,
        VoteType voteType,
        String title,
        OffsetDateTime createdAt,
        String content,
        String thumbnailUrl,
        String status,
        OffsetDateTime endAt,
        int participantCount,
        List<OptionItem> options,
        MyVote myVote,
        EmojiSummary emojiSummary,
        String myEmoji,
        int commentCount
) {
    public record OptionItem(Long optionId, String label, Long voteCount, Integer ratio) {
    }

    public record MyVote(boolean voted, Long selectedOptionId) {
    }

    public record EmojiSummary(long LIKE, long SAD, long ANGRY, long WOW) {
        public static EmojiSummary from(Map<VoteEmoji, Long> map) {
            return new EmojiSummary(
                    map.getOrDefault(VoteEmoji.LIKE, 0L),
                    map.getOrDefault(VoteEmoji.SAD, 0L),
                    map.getOrDefault(VoteEmoji.ANGRY, 0L),
                    map.getOrDefault(VoteEmoji.WOW, 0L)
            );
        }
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static VoteDetailResponse from(VoteDetailResult result) {
        // 투표 전이면 voteCount/ratio를 null로
        boolean showResults = result.voted();
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(
                        o.optionId(),
                        o.label(),
                        showResults ? o.voteCount() : null,
                        showResults ? o.ratio() : null
                ))
                .toList();
        String myEmoji = result.myEmoji() != null ? result.myEmoji().name() : null;
        MyVote myVote = new MyVote(result.voted(), result.mySelectedOptionId());
        EmojiSummary emojiSummary = EmojiSummary.from(result.emojiSummary());

        return new VoteDetailResponse(
                result.voteId(),
                result.type(),
                result.title(),
                toKst(result.createdAt()),
                result.content(),
                result.thumbnailUrl(),
                result.status().name(),
                toKst(result.endAt()),
                result.participantCount(),
                items,
                myVote,
                emojiSummary,
                myEmoji,
                result.commentCount()
        );
    }
}
