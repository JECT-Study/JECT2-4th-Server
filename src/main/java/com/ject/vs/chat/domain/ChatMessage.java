package com.ject.vs.chat.domain;

import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.common.domain.BaseAggregateEntity;
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

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.TEXT;

    private Long parentMessageId;

    public static ChatMessage of(Long voteId, Long senderId, String content) {
        return of(voteId, senderId, content, null);
    }

    public static ChatMessage of(Long voteId, Long senderId, String content, Long parentMessageId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.voteId = voteId;
        chatMessage.senderId = senderId;
        chatMessage.content = content;
        chatMessage.messageType = MessageType.TEXT;
        chatMessage.parentMessageId = parentMessageId;
        chatMessage.registerEvent(new ChatMessageSentEvent(chatMessage));
        return chatMessage;
    }

    public static ChatMessage ofSystem(Long voteId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.voteId = voteId;
        chatMessage.senderId = 0L; // sentinel: ChatService + Listener must guard getUser(0) / isMine
        chatMessage.content = content;
        chatMessage.messageType = MessageType.SYSTEM;
        chatMessage.registerEvent(new ChatMessageSentEvent(chatMessage));
        return chatMessage;
    }

    public MessageType getMessageType() {
        return messageType != null ? messageType : MessageType.TEXT;
    }

    public boolean isBlank() {
        return content.isBlank();
    }
}
