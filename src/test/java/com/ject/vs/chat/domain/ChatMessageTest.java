package com.ject.vs.chat.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageTest {

    @Test
    void of_팩토리로_생성시_voteId_senderId_content가_올바르게_설정된다() {
        Long voteId = 1L;
        Long senderId = 2L;
        String content = "안녕하세요";

        ChatMessage chatMessage = ChatMessage.of(voteId, senderId, content);

        assertThat(chatMessage.getVoteId()).isEqualTo(voteId);
        assertThat(chatMessage.getSenderId()).isEqualTo(senderId);
        assertThat(chatMessage.getContent()).isEqualTo(content);
    }

    @Test
    void isBlank_공백만_있으면_true를_반환한다() {
        ChatMessage chatMessage = ChatMessage.of(1L, 2L, "   ");

        assertThat(chatMessage.isBlank()).isTrue();
    }

    @Test
    void isBlank_내용이_있으면_false를_반환한다() {
        ChatMessage chatMessage = ChatMessage.of(1L, 2L, "hello");

        assertThat(chatMessage.isBlank()).isFalse();
    }
}
