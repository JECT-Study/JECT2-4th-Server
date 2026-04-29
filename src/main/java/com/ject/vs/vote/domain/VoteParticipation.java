package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "vote_participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteParticipation extends BaseEntity {

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private Long userId;

    public static VoteParticipation of(Long voteId, Long userId) {
        VoteParticipation voteParticipation = new VoteParticipation();
        voteParticipation.voteId = voteId;
        voteParticipation.userId = userId;
        return voteParticipation;
    }
}
