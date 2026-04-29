package com.ject.vs.chat.domain;

import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.common.domain.AggregateRoot;
import com.ject.vs.common.domain.BaseTimeEntity;
import com.ject.vs.common.infrastructure.DomainEventEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "chat_message")
@EntityListeners(DomainEventEntityListener.class)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ChatMessage extends BaseTimeEntity implements AggregateRoot {

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String content;

    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    public static ChatMessage of(Long voteId, Long senderId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.voteId = voteId;
        chatMessage.senderId = senderId;
        chatMessage.content = content;
        chatMessage.domainEvents.add(new ChatMessageSentEvent(chatMessage));
        return chatMessage;
    }

    public boolean isBlank() {
        return content.isBlank();
    }

    @Override
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @Override
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
