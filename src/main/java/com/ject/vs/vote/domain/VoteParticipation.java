package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "vote_participation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"vote_id", "user_id"}),
                @UniqueConstraint(name = "uq_guest_vote", columnNames = {"vote_id", "anonymous_id"})
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteParticipation extends BaseTimeEntity {

    @Column(nullable = false)
    private Long voteId;

    private Long userId;

    private String anonymousId;

    @Column(nullable = false)
    private Long optionId;

    public static VoteParticipation ofMember(Long voteId, Long userId, Long optionId) {
        VoteParticipation p = new VoteParticipation();
        p.voteId = voteId;
        p.userId = userId;
        p.optionId = optionId;
        return p;
    }

    public static VoteParticipation ofGuest(Long voteId, String anonymousId, Long optionId) {
        VoteParticipation p = new VoteParticipation();
        p.voteId = voteId;
        p.anonymousId = anonymousId;
        p.optionId = optionId;
        return p;
    }

    public boolean isGuest() {
        return anonymousId != null;
    }

    public void changeOption(Long optionId) {
        this.optionId = optionId;
    }
}
