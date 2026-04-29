package com.ject.vs.chat.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "chat_room_unread")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ChatRoomUnread {

    @EmbeddedId
    private ChatRoomUnreadId id;

    private Long lastReadMessageId;

    private LocalDateTime lastReadAt;

    public static ChatRoomUnread of(Long userId, Long voteId, Long lastReadMessageId) {
        ChatRoomUnread chatRoomUnread = new ChatRoomUnread();
        chatRoomUnread.id = ChatRoomUnreadId.of(userId, voteId);
        chatRoomUnread.lastReadMessageId = lastReadMessageId;
        chatRoomUnread.lastReadAt = LocalDateTime.now();
        return chatRoomUnread;
    }

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = LocalDateTime.now();
    }
}
