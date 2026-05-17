package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.VoteResultQueryUseCase.AiInsightView;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.GenderDistribution;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.Insight;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record VoteResultResponse(
        Long voteId,
        String title,
        OffsetDateTime createdAt,
        String content,
        String thumbnailUrl,
        String status,
        OffsetDateTime endAt,
        int participantCount,
        ResultOptions result,
        MyVote myVote,
        InsightResponse insight,
        AiInsightResponse aiInsight
) {
    public record ResultOptions(List<OptionItem> options) {
    }

    public record OptionItem(Long optionId, String label, long voteCount, int ratio) {
    }

    public record MyVote(boolean voted, Long selectedOptionId) {
    }

    public record InsightResponse(
            boolean locked,
            String scope,
            Integer selectionCount,
            GenderDistributionResponse genderDistribution,
            List<AgeDistributionResponse> ageDistribution
    ) {
        public record GenderDistributionResponse(
                GenderDetail female,
                GenderDetail male
        ) {
        }

        public record GenderDetail(long count, int ratio) {
        }

        public record AgeDistributionResponse(String ageGroup, int ratio, boolean isMyGroup) {
        }

        static InsightResponse from(Insight insight) {
            if (insight == null) return null;
            if (insight.locked()) return new InsightResponse(true, null, null, null, null);

            GenderDistributionResponse gender = null;
            if (insight.genderDistribution() != null) {
                GenderDistribution g = insight.genderDistribution();
                gender = new GenderDistributionResponse(
                        new GenderDetail(g.femaleCount(), g.femaleRatio()),
                        new GenderDetail(g.maleCount(), g.maleRatio())
                );
            }

            List<AgeDistributionResponse> ages = null;
            if (insight.ageDistribution() != null) {
                ages = insight.ageDistribution().stream()
                        .map(a -> new AgeDistributionResponse(a.ageGroup(), a.ratio(), a.isMyGroup()))
                        .toList();
            }

            return new InsightResponse(false, insight.scope() != null ? insight.scope().name() : null,
                    insight.selectionCount(), gender, ages);
        }
    }

    public record AiInsightResponse(boolean available, String headline, String body) {
        static AiInsightResponse from(AiInsightView view) {
            if (view == null) return new AiInsightResponse(false, null, null);
            return new AiInsightResponse(view.available(), view.headline(), view.body());
        }
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static VoteResultResponse from(VoteResultDetail result) {
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                .toList();
        return new VoteResultResponse(
                result.voteId(),
                result.title(),
                toKst(result.createdAt()),
                result.content(),
                result.thumbnailUrl(),
                result.status().name(),
                toKst(result.endAt()),
                result.participantCount(),
                new ResultOptions(items),
                new MyVote(result.voted(), result.mySelectedOptionId()),
                InsightResponse.from(result.insight()),
                AiInsightResponse.from(result.aiInsight())
        );
    }
}
