package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.domain.ChatReactionType;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.ReplyInfo;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.vote.domain.VoteOptionCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "채팅 메시지 응답")
public record MessageResponse(
        @Schema(description = "채팅 메시지 ID", example = "128")
        Long messageId,

        @Schema(description = "메시지를 보낸 사용자의 ID", example = "42")
        Long senderId,

        @Schema(description = "메시지 내용", example = "저는 A가 좋아요")
        String content,

        @Schema(description = "메시지 전송 시각. UTC 기준으로 내려가며 사용자 시간대에 맞춰 변환이 필요합니다.", example = "2026-05-01T06:30:00Z")
        Instant sentAt,

        @Schema(description = "메시지를 보낸 사용자의 닉네임", example = "승부사")
        String senderNickname,

        @Schema(description = "메시지를 보낸 사용자의 프로필 아이콘 색상", example = "GREEN", allowableValues = {"GREEN", "RED", "BLUE", "YELLOW"}, nullable = true)
        ImageColor senderProfileIcon,

        @Schema(description = "메시지를 보낸 사용자가 선택한 투표 선택지", example = "A", allowableValues = {"A", "B"}, nullable = true)
        VoteOptionCode senderVoteOption,

        @Schema(description = "현재 로그인 사용자가 보낸 메시지인지 여부", example = "false")
        boolean isMine,

        @Schema(description = "답글 대상 메시지 정보 (답글이 아닌 경우 null)", nullable = true)
        ReplyInfo replyTo,

        @Schema(description = "이모지 반응 카운트", example = "{\"THUMBS_UP\": 5, \"THUMBS_DOWN\": 1}")
        Map<ChatReactionType, Long> reactions,

        @Schema(description = "현재 사용자가 선택한 반응 (없으면 null)", nullable = true)
        ChatReactionType myReaction
) {
    public static MessageResponse from(MessageResult result) {
        return new MessageResponse(
                result.messageId(),
                result.senderId(),
                result.content(),
                result.sentAt(),
                result.senderNickname(),
                result.senderProfileIcon(),
                result.senderVoteOption(),
                result.isMine(),
                result.replyTo(),
                result.reactions() != null ? result.reactions() : Map.of(),
                result.myReaction()
        );
    }
}
