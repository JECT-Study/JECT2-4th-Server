package com.ject.vs.chat.domain;

import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.common.domain.BaseAggregateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ChatMessage extends BaseAggregateEntity {

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String content;

    public static ChatMessage of(Long voteId, Long senderId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.voteId = voteId;
        chatMessage.senderId = senderId;
        chatMessage.content = content;
        chatMessage.registerEvent(new ChatMessageSentEvent(chatMessage));
        return chatMessage;
    }

    public boolean isBlank() {
        return content.isBlank();
    }
}
