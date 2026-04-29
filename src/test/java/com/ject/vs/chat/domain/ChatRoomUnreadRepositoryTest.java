package com.ject.vs.chat.domain;

import com.ject.vs.domain.User;
import com.ject.vs.vote.domain.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatRoomUnreadRepositoryTest {

    @Autowired
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long userId;
    private Long voteId;

    @BeforeEach
    void setUp() {
        User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
        Vote vote = entityManager.persistAndFlush(Vote.createForTest());
        userId = user.getId();
        voteId = vote.getId();
    }

    @Nested
    class findByIdUserIdAndIdVoteId {

        @Test
        void 저장_후_조회할_수_있다() {
            // given
            chatRoomUnreadRepository.save(ChatRoomUnread.of(userId, voteId, 5L));

            // when
            Optional<ChatRoomUnread> result = chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(userId, voteId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getLastReadMessageId()).isEqualTo(5L);
        }
    }

    @Nested
    class save {

        @Test
        void 재호출시_lastReadMessageId가_갱신된다() {
            // given
            chatRoomUnreadRepository.save(ChatRoomUnread.of(userId, voteId, 5L));
            ChatRoomUnread saved = chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(userId, voteId).get();
            saved.updateLastRead(20L);

            // when
            chatRoomUnreadRepository.save(saved);

            // then
            Optional<ChatRoomUnread> result = chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(userId, voteId);
            assertThat(result).isPresent();
            assertThat(result.get().getLastReadMessageId()).isEqualTo(20L);
        }
    }
}
