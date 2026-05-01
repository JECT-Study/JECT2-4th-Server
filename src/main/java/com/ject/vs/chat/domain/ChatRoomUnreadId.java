package com.ject.vs.chat.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class ChatRoomUnreadId implements Serializable {

    private Long userId;
    private Long voteId;

    public static ChatRoomUnreadId of(Long userId, Long voteId) {
        ChatRoomUnreadId id = new ChatRoomUnreadId();
        id.userId = userId;
        id.voteId = voteId;
        return id;
    }
}
