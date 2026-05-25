package com.ject.vs.chat.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageTest {

    @Nested
    class of {

        @Test
        void voteId_senderId_content가_올바르게_설정된다() {
            // given
            Long voteId = 1L;
            Long senderId = 2L;
            String content = "안녕하세요";

            // when
            ChatMessage chatMessage = ChatMessage.of(voteId, senderId, content);

            // then
            assertThat(chatMessage.getVoteId()).isEqualTo(voteId);
            assertThat(chatMessage.getSenderId()).isEqualTo(senderId);
            assertThat(chatMessage.getContent()).isEqualTo(content);
        }
    }

    @Nested
    class isBlank {

        @Test
        void 공백만_있으면_true를_반환한다() {
            // given
            ChatMessage chatMessage = ChatMessage.of(1L, 2L, "   ");

            // when
            boolean result = chatMessage.isBlank();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void 내용이_있으면_false를_반환한다() {
            // given
            ChatMessage chatMessage = ChatMessage.of(1L, 2L, "hello");

            // when
            boolean result = chatMessage.isBlank();

            // then
            assertThat(result).isFalse();
        }
    }
}
