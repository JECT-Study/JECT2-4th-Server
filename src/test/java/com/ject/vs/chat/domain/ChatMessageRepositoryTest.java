package com.ject.vs.chat.domain;

import com.ject.vs.domain.User;
import com.ject.vs.vote.domain.Vote;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void findByVoteIdOrderByIdDesc_최신순으로_정렬된다() {
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

        List<ChatMessage> result = chatMessageRepository.findByVoteIdOrderByIdDesc(voteId, PageRequest.of(0, 10));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
        assertThat(result.get(1).getId()).isGreaterThan(result.get(2).getId());
    }

    @Test
    void findByVoteIdAndIdLessThanOrderByIdDesc_cursor_기반_페이지네이션() {
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
        ChatMessage msg3 = chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

        List<ChatMessage> result = chatMessageRepository
                .findByVoteIdAndIdLessThanOrderByIdDesc(voteId, msg3.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result).doesNotContain(msg3);
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
    }

    @Test
    void countByVoteIdAndIdGreaterThan_unread_count를_계산한다() {
        ChatMessage msg1 = chatMessageRepository.save(ChatMessage.of(voteId, userId, "첫번째"));
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "두번째"));
        chatMessageRepository.save(ChatMessage.of(voteId, userId, "세번째"));

        long count = chatMessageRepository.countByVoteIdAndIdGreaterThan(voteId, msg1.getId());

        assertThat(count).isEqualTo(2);
    }
}
