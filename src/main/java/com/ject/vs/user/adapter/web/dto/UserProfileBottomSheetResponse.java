package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.domain.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "다른 유저 프로필 바텀시트 응답")
public record UserProfileBottomSheetResponse(
        @Schema(description = "프로필 대상 유저 ID", example = "42")
        Long userId,

        @Schema(description = "닉네임", example = "승부사")
        String nickname,

        @Schema(description = "프로필 아이콘 색상", example = "GREEN", nullable = true)
        ImageColor imageColor,

        @Schema(description = "참여투표 총 개수", example = "24")
        long participatedVoteCount,

        @Schema(description = "최신 활동순 참여투표 (최대 3개)")
        List<ParticipatedVoteCard> recentParticipatedVotes
) {
    @Schema(description = "참여투표 카드")
    public record ParticipatedVoteCard(
            @Schema(description = "투표 ID", example = "101")
            Long voteId,

            @Schema(description = "투표 제목", example = "직장인 점심시간 혼밥 vs 같이 먹기")
            String title,

            @Schema(description = "투표 상태", example = "ONGOING")
            VoteStatus status,

            @Schema(description = "투표 타입", example = "GENERAL")
            VoteType voteType,

            @Schema(description = "프로필 대상 유저가 선택한 선택지 텍스트", example = "혼밥이 편하다")
            String selectedOptionLabel,

            @Schema(description = "조회자(본인)의 해당 투표 참여 여부", example = "true")
            boolean viewerParticipated
    ) {}
}