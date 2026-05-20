package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.InsightScope;
import com.ject.vs.vote.domain.VoteStatus;

import java.time.Instant;
import java.util.List;

public interface VoteResultQueryUseCase {

    VoteResultDetail getResult(Long voteId, Long userId);

    ShareLinkResult getShareLink(Long voteId);

    record VoteResultDetail(
            Long voteId,
            String title,
            Instant createdAt,
            String content,
            String thumbnailUrl,
            VoteStatus status,
            Instant endAt,
            int participantCount,
            List<VoteCommandUseCase.OptionResult> options,
            boolean voted,
            Long mySelectedOptionId,
            Insight insight,
            AiInsightView aiInsight
    ) {
    }

    record Insight(
            boolean locked,
            InsightScope scope,
            Integer selectionCount,
            GenderDistribution genderDistribution,
            List<AgeDistribution> ageDistribution
    ) {
        public static Insight ofLocked() {
            return new Insight(true, null, null, null, null);
        }
    }

    /**
     * 성별 분포 정보
     * @param total 도넛 차트 중앙에 표시할 인원 수 (참여자: 해당 선택 인원, 미참여자: 전체 인원)
     * @param femaleCount 여성 수 (참여자: 해당 선택의 여성, 미참여자: 전체 여성)
     * @param femaleRatio 전체 투표에서 여성이 차지하는 비율 (%)
     * @param maleCount 남성 수 (참여자: 해당 선택의 남성, 미참여자: 전체 남성)
     * @param maleRatio 전체 투표에서 남성이 차지하는 비율 (%)
     * @param highlightedGender 강조할 성별 (참여자: 사용자 성별, 미참여자: 다수 성별)
     */
    record GenderDistribution(
            int total,
            long femaleCount,
            int femaleRatio,
            long maleCount,
            int maleRatio,
            String highlightedGender
    ) {
    }

    /**
     * 연령대 분포 정보
     * @param ageGroup 연령대 레이블 (10s, 20s, 30s, 40s, 50s_PLUS)
     * @param ratio 해당 연령대 비율 (%)
     * @param isHighlighted 강조 여부 (참여자: 내 그룹 여부, 미참여자: 다수 그룹 여부)
     */
    record AgeDistribution(String ageGroup, int ratio, boolean isHighlighted) {
    }

    record AiInsightView(boolean available, String headline, String body) {
        public static AiInsightView of(String headline, String body) {
            return new AiInsightView(true, headline, body);
        }

        public static AiInsightView unavailable() {
            return new AiInsightView(false, null, null);
        }
    }

    record ShareLinkResult(String shareUrl, String title, String thumbnailUrl) {
    }
}
