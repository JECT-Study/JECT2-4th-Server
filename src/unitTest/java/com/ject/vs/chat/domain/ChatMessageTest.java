package com.ject.vs.chat.domain;

import com.ject.vs.user.domain.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ChatMessageTest {

    @Nested
    class of {

        @Test
        void voteId_sender_content가_올바르게_설정된다() {
            Long voteId = 1L;
            Long senderId = 2L;
            String content = "안녕하세요";
            User sender = mock(User.class);
            given(sender.getId()).willReturn(senderId);

            ChatMessage chatMessage = ChatMessage.of(voteId, sender, content);

            assertThat(chatMessage.getVoteId()).isEqualTo(voteId);
            assertThat(chatMessage.getSenderId()).isEqualTo(senderId);
            assertThat(chatMessage.getContent()).isEqualTo(content);
            assertThat(chatMessage.getSender()).isEqualTo(sender);
        }
    }

    @Nested
    class isBlank {

        @Test
        void 공백만_있으면_true를_반환한다() {
            User sender = mock(User.class);
            ChatMessage chatMessage = ChatMessage.of(1L, sender, "   ");

            assertThat(chatMessage.isBlank()).isTrue();
        }

        @Test
        void 내용이_있으면_false를_반환한다() {
            User sender = mock(User.class);
            ChatMessage chatMessage = ChatMessage.of(1L, sender, "hello");

            assertThat(chatMessage.isBlank()).isFalse();
        }
    }
}