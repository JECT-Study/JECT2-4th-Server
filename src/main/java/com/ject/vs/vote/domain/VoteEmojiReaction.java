package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "vote_emoji_reaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_member_emoji", columnNames = {"vote_id", "user_id"}),
                @UniqueConstraint(name = "uq_guest_emoji", columnNames = {"vote_id", "anonymous_id"})
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteEmojiReaction extends BaseTimeEntity {

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id")
    private String anonymousId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteEmoji emoji;

    public static VoteEmojiReaction ofMember(Long voteId, Long userId, VoteEmoji emoji) {
        VoteEmojiReaction r = new VoteEmojiReaction();
        r.voteId = voteId;
        r.userId = userId;
        r.emoji = emoji;
        return r;
    }

    public static VoteEmojiReaction ofGuest(Long voteId, String anonymousId, VoteEmoji emoji) {
        VoteEmojiReaction r = new VoteEmojiReaction();
        r.voteId = voteId;
        r.anonymousId = anonymousId;
        r.emoji = emoji;
        return r;
    }

    public void changeEmoji(VoteEmoji newEmoji) {
        this.emoji = newEmoji;
    }
}
