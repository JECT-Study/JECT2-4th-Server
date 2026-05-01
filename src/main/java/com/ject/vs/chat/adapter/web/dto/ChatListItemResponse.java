package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "채팅방 목록 항목 응답")
public record ChatListItemResponse(
        @Schema(description = "채팅방이 연결된 투표 ID", example = "1")
        Long voteId,

        @Schema(description = "투표 제목", example = "오늘 점심 메뉴는?")
        String title,

        @Schema(description = "투표 썸네일 이미지 URL", example = "https://cdn.example.com/votes/1/thumbnail.png", nullable = true)
        String thumbnailUrl,

        @Schema(description = "A 선택지 이름", example = "김치찌개")
        String optionA,

        @Schema(description = "B 선택지 이름", example = "된장찌개")
        String optionB,

        @Schema(description = "투표 참여자 수", example = "42")
        int participantCount,

        @Schema(description = "채팅방의 마지막 메시지 내용", example = "저는 A가 좋아요", nullable = true)
        String lastMessage,

        @Schema(description = "마지막 메시지가 전송된 시각. UTC 기준으로 내려가며 사용자 시간대에 맞춰 변환이 필요합니다.", example = "2026-05-01T06:30:00Z", nullable = true)
        Instant lastMessageAt,

        @Schema(description = "투표 종료 시각. UTC 기준으로 내려가며 사용자 시간대에 맞춰 변환이 필요합니다.", example = "2026-05-02T15:00:00Z")
        Instant endAt,

        @Schema(description = "현재 사용자가 아직 읽지 않은 메시지 수", example = "3")
        int unreadCount
) {
    public static ChatListItemResponse from(ChatListItemResult result) {
        return new ChatListItemResponse(
                result.voteId(),
                result.title(),
                result.thumbnailUrl(),
                result.optionA(),
                result.optionB(),
                result.participantCount(),
                result.lastMessage(),
                result.lastMessageAt(),
                result.endAt(),
                result.unreadCount()
        );
    }
}
