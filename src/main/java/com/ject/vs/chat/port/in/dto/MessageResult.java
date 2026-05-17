package com.ject.vs.chat.port.in.dto;

import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.vote.domain.VoteOptionCode;

import java.time.Instant;

public record MessageResult(
        Long messageId,
        String content,
        Instant sentAt,
        String senderNickname,
        ImageColor senderProfileIcon,
        VoteOptionCode senderVoteOption,
        boolean isMine
) {}
