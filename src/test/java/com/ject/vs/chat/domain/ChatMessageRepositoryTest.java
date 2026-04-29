package com.ject.vs.chat.domain;

import com.ject.vs.domain.User;
import com.ject.vs.vote.domain.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long voteId;
    private Long userId;

    @BeforeEach
    void setUp() {
        User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
        Vote vote = entityManager.persistAndFlush(Vote.createForTest());
        voteId = vote.getId();
        userId = user.getId();
    }

    @Nested
    class findByVoteIdOrderByIdDesc {

        @Test
        void 최신순으로_정렬된다() {
            // given
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

            // when
            List<ChatMessage> result = chatMessageRepository.findByVoteIdOrderByIdDesc(voteId, PageRequest.of(0, 10));

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
            assertThat(result.get(1).getId()).isGreaterThan(result.get(2).getId());
        }
    }

    @Nested
    class findByVoteIdAndIdLessThanOrderByIdDesc {

        @Test
        void cursor_기반으로_이전_메시지를_페이지네이션한다() {
            // given
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
            ChatMessage msg3 = chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

            // when
            List<ChatMessage> result = chatMessageRepository
                    .findByVoteIdAndIdLessThanOrderByIdDesc(voteId, msg3.getId(), PageRequest.of(0, 10));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).doesNotContain(msg3);
            assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
        }
    }

    @Nested
    class countByVoteIdAndIdGreaterThan {

        @Test
        void unread_count를_계산한다() {
            // given
            ChatMessage msg1 = chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
            chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

            // when
            long count = chatMessageRepository.countByVoteIdAndIdGreaterThan(voteId, msg1.getId());

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
