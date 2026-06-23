package com.ject.vs.chat.domain;

import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.common.domain.BaseAggregateEntity;
import com.ject.vs.user.domain.User;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private ChatMessage parentMessage;

    public static ChatMessage of(Long voteId, User sender, String content) {
        return of(voteId, sender, content, null);
    }

    public static ChatMessage of(Long voteId, User sender, String content, ChatMessage parentMessage) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.voteId = voteId;
        chatMessage.sender = sender;
        chatMessage.content = content;
        chatMessage.parentMessage = parentMessage;
        chatMessage.registerEvent(new ChatMessageSentEvent(chatMessage));
        return chatMessage;
    }

    public Long getSenderId() {
        return sender.getId();
    }

    public Long getParentMessageId() {
        return parentMessage != null ? parentMessage.getId() : null;
    }

    public boolean isBlank() {
        return content.isBlank();
    }
}