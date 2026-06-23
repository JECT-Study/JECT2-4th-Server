package com.ject.vs.chat.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "chat_message_reaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_chat_reaction", columnNames = {"message_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ChatMessageReaction extends BaseTimeEntity {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatReactionType emoji;

    public static ChatMessageReaction of(Long messageId, Long userId, ChatReactionType emoji) {
        ChatMessageReaction reaction = new ChatMessageReaction();
        reaction.messageId = messageId;
        reaction.userId = userId;
        reaction.emoji = emoji;
        return reaction;
    }

    public void changeEmoji(ChatReactionType newEmoji) {
        this.emoji = newEmoji;
    }
}
