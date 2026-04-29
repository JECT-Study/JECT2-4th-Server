package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.port.in.MessagePageResult;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    @InjectMocks
    private ChatQueryService chatQueryService;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    class getMessages {

        @Test
        void cursor가_null이면_최신부터_조회한다() {
            // given
            ChatMessage msg = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.findByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            given(userRepository.findById(2L)).willReturn(Optional.empty());

            // when
            MessagePageResult result = chatQueryService.getMessages(1L, 2L, null, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class));
        }

        @Test
        void cursor가_있으면_cursor_이전_메시지를_조회한다() {
            // given
            ChatMessage msg = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.findByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            given(userRepository.findById(2L)).willReturn(Optional.empty());

            // when
            MessagePageResult result = chatQueryService.getMessages(1L, 2L, 100L, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class));
        }
    }
}
