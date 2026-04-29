package com.ject.vs.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUnreadRepository extends JpaRepository<ChatRoomUnread, ChatRoomUnreadId> {

    Optional<ChatRoomUnread> findByIdUserIdAndIdVoteId(Long userId, Long voteId);

    List<ChatRoomUnread> findAllByIdVoteId(Long voteId);
}
