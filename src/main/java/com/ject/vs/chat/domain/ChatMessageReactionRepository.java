package com.ject.vs.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReaction, Long> {

    Optional<ChatMessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

    void deleteByMessageIdAndUserId(Long messageId, Long userId);

    @Query("""
        SELECT new com.ject.vs.chat.domain.ReactionCount(r.messageId, r.emoji, COUNT(r))
        FROM ChatMessageReaction r
        WHERE r.messageId IN :messageIds
        GROUP BY r.messageId, r.emoji
    """)
    List<ReactionCount> countByMessageIds(@Param("messageIds") List<Long> messageIds);

    @Query("""
        SELECT new com.ject.vs.chat.domain.MyReaction(r.messageId, r.emoji)
        FROM ChatMessageReaction r
        WHERE r.messageId IN :messageIds AND r.userId = :userId
    """)
    List<MyReaction> findMyReactionsByMessageIds(@Param("messageIds") List<Long> messageIds, @Param("userId") Long userId);
}
