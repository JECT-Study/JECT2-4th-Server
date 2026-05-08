package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.Optional;

public interface VoteEmojiReactionRepository extends JpaRepository<VoteEmojiReaction, Long> {

    Optional<VoteEmojiReaction> findByVoteIdAndUserId(Long voteId, Long userId);

    Optional<VoteEmojiReaction> findByVoteIdAndAnonymousId(Long voteId, String anonymousId);

    @Query("SELECT r.emoji, COUNT(r) FROM VoteEmojiReaction r WHERE r.voteId = :voteId GROUP BY r.emoji")
    java.util.List<Object[]> countByEmojiForVote(@Param("voteId") Long voteId);

    long countByVoteId(Long voteId);
}
