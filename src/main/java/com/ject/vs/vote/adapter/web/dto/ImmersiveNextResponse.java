package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveNextResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public record ImmersiveNextResponse(List<VoteItem> items) {

    public record VoteItem(
            Long voteId,
            String title,
            String content,
            String imageUrl,
            OffsetDateTime endAt,
            List<OptionItem> options,
            MyVote myVote,
            EmojiSummary emojiSummary,
            String myEmoji,
            int commentCount,
            int currentViewerCount
    ) {
    }

    public record OptionItem(Long optionId, String label, Long voteCount, Integer ratio) {
    }

    public record MyVote(boolean voted, Long selectedOptionId) {
    }

    public record EmojiSummary(long LIKE, long SAD, long ANGRY, long WOW, long total) {
        public static EmojiSummary from(Map<VoteEmoji, Long> map, long total) {
            return new EmojiSummary(
                    map.getOrDefault(VoteEmoji.LIKE, 0L),
                    map.getOrDefault(VoteEmoji.SAD, 0L),
                    map.getOrDefault(VoteEmoji.ANGRY, 0L),
                    map.getOrDefault(VoteEmoji.WOW, 0L),
                    total
            );
        }
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static ImmersiveNextResponse from(ImmersiveNextResult result) {
        List<VoteItem> items = result.items().stream()
                .map(i -> {
                    List<OptionItem> options = i.options().stream()
                            .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                            .toList();
                    MyVote myVote = new MyVote(i.voted(), i.mySelectedOptionId());
                    EmojiSummary emojiSummary = EmojiSummary.from(i.emojiSummary(), i.emojiTotal());
                    String myEmoji = i.myEmoji() != null ? i.myEmoji().name() : null;

                    return new VoteItem(
                            i.voteId(),
                            i.title(),
                            i.content(),
                            i.imageUrl(),
                            toKst(i.endAt()),
                            options,
                            myVote,
                            emojiSummary,
                            myEmoji,
                            i.commentCount(),
                            i.currentViewerCount()
                    );
                })
                .toList();
        return new ImmersiveNextResponse(items);
    }
}
