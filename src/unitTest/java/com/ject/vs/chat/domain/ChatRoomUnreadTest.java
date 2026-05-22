package com.ject.vs.chat.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomUnreadTest {

    @Nested
    class updateLastRead {

        @Test
        void lastReadMessageId와_lastReadAt이_갱신된다() {
            // given
            ChatRoomUnread unread = ChatRoomUnread.of(1L, 10L, 5L);

            // when
            unread.updateLastRead(20L);

            // then
            assertThat(unread.getLastReadMessageId()).isEqualTo(20L);
            assertThat(unread.getLastReadAt()).isNotNull();
        }
    }
}
