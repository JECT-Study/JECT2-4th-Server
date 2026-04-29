package com.ject.vs.chat.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomUnreadTest {

    @Test
    void updateLastRead_호출시_lastReadMessageId와_lastReadAt이_갱신된다() {
        ChatRoomUnread unread = ChatRoomUnread.of(1L, 10L, 5L);

        unread.updateLastRead(20L);

        assertThat(unread.getLastReadMessageId()).isEqualTo(20L);
        assertThat(unread.getLastReadAt()).isNotNull();
    }
}
