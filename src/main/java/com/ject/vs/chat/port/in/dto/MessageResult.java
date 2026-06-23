package com.ject.vs.chat.port.in.dto;

import com.ject.vs.chat.domain.ChatReactionType;
import com.ject.vs.chat.domain.MessageType;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.vote.domain.VoteOptionCode;

import java.time.Instant;
import java.util.Map;

public record MessageResult(
        Long messageId,
        String content,
        Instant sentAt,
        String senderNickname,
        ImageColor senderProfileIcon,
        VoteOptionCode senderVoteOption,
        boolean isMine,
        // 행동 로그(chat_message_sent)의 is_first_message 변수: 해당 채팅방의 첫 메시지인지 여부.
        // 메시지 전송 시점에만 의미를 가지며, 목록 조회/실시간 브로드캐스트에서는 false로 채운다.
        boolean isFirstMessage,
        MessageType messageType,

        // 답글 정보 (답글이 아니면 null)
        ReplyInfo replyTo,

        // 이모지 반응 카운트 (THUMBS_UP, THUMBS_DOWN)
        Map<ChatReactionType, Long> reactionCounts,

        // 현재 사용자가 누른 반응 (없으면 null). REST 조회 시에만 채워짐
        ChatReactionType myReaction
) {}
