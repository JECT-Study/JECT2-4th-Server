package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatRoomResult;
import com.ject.vs.vote.domain.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "채팅방 상세 응답")
public record ChatRoomResponse(
        @Schema(description = "채팅방이 연결된 투표 ID", example = "1")
        Long voteId,

        @Schema(description = "투표 제목", example = "오늘 점심 메뉴는?")
        String title,

        @Schema(description = "투표 진행 상태", example = "ONGOING", allowableValues = {"ONGOING", "ENDED"})
        VoteStatus status,

        @Schema(description = "투표 참여자 수", example = "42")
        int participantCount,

        @Schema(description = "A 선택지 이름", example = "김치찌개")
        String optionA,

        @Schema(description = "B 선택지 이름", example = "된장찌개")
        String optionB,

        @Schema(description = "투표 종료 시각. UTC 기준으로 내려가며 사용자 시간대에 맞춰 변환이 필요합니다.", example = "2026-05-02T15:00:00Z")
        Instant endAt
) {
    public static ChatRoomResponse from(ChatRoomResult result) {
        return new ChatRoomResponse(
                result.voteId(),
                result.title(),
                result.status(),
                result.participantCount(),
                result.optionA(),
                result.optionB(),
                result.endAt()
        );
    }
}
