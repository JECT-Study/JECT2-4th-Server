package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VoteEmojiReactionRepository extends JpaRepository<VoteEmojiReaction, Long> {

    Optional<VoteEmojiReaction> findByVoteIdAndUserId(Long voteId, Long userId);

    Optional<VoteEmojiReaction> findByVoteIdAndAnonymousId(Long voteId, String anonymousId);

    @Query("""
                SELECT new com.ject.vs.vote.domain.EmoijCount(r.emoji, COUNT(r))
                FROM VoteEmojiReaction r 
                WHERE r.voteId = :voteId 
                GROUP BY r.emoji
            """)
    List<EmoijCount> countByEmojiForVote(@Param("voteId") Long voteId);

    long countByVoteId(Long voteId);
}
