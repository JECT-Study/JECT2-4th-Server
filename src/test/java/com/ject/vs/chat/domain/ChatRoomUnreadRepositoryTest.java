package com.ject.vs.chat.domain;

import com.ject.vs.config.JpaAuditingConfig;
import com.ject.vs.user.domain.User;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ChatRoomUnreadRepositoryTest {

    @Autowired
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long userId;
    private Long voteId;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        User user = entityManager.persistAndFlush(User.createWithSub("test-sub"));
        Vote vote = entityManager.persistAndFlush(
                Vote.create(VoteType.GENERAL, "테스트", null, "thumb", null, Duration.ofHours(24), FIXED_CLOCK)
        );
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
