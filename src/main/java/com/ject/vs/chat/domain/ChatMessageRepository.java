package com.ject.vs.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.voteId = :voteId AND m.id < :cursor
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findAllByVoteIdAndIdLessThanOrderByIdDesc(
            @Param("voteId") Long voteId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.voteId = :voteId
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findAllByVoteIdOrderByIdDesc(@Param("voteId") Long voteId, Pageable pageable);

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.id = :id
            """)
    Optional<ChatMessage> findByIdWithSender(@Param("id") Long id);

    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.id IN :ids
            """)
    List<ChatMessage> findAllByIdWithSender(@Param("ids") Collection<Long> ids);

    Optional<ChatMessage> findFirstByVoteIdOrderByIdDesc(Long voteId);

    long countByVoteIdAndIdGreaterThan(Long voteId, Long lastReadMessageId);

    long countByVoteId(Long voteId);
}