package com.ject.vs.chat.port.in;

import com.ject.vs.chat.domain.ChatReactionType;
import com.ject.vs.chat.port.in.dto.MarkAsReadCommand;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.ReactionResult;
import com.ject.vs.chat.port.in.dto.SendMessageCommand;

public interface ChatCommandUseCase {
    MessageResult sendMessage(SendMessageCommand command);
    void markAsRead(MarkAsReadCommand command);
    void sendSystemMessage(Long voteId, String content);

    /**
     * 메시지에 반응(이모지)을 추가/변경/취소합니다.
     * emoji가 null이면 취소.
     */
    ReactionResult reactToMessage(Long voteId, Long userId, Long messageId, ChatReactionType emoji);
}
