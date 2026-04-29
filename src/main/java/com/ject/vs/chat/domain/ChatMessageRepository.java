package com.ject.vs.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByVoteIdAndIdLessThanOrderByIdDesc(Long voteId, Long cursor, Pageable pageable);

    List<ChatMessage> findAllByVoteIdOrderByIdDesc(Long voteId, Pageable pageable);

    Optional<ChatMessage> findFirstByVoteIdOrderByIdDesc(Long voteId);

    long countByVoteIdAndIdGreaterThan(Long voteId, Long lastReadMessageId);
}
