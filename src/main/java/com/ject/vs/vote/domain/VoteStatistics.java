package com.ject.vs.vote.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "vote_statistics")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteStatistics {

    @Id
    private Long voteId;

    @Column(nullable = false)
    private Long viewCount;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vote_id")
    private Vote vote;

    public static VoteStatistics create(Vote vote) {
        VoteStatistics stats = new VoteStatistics();
        stats.vote = vote;
        stats.voteId = vote.getId();
        stats.viewCount = 0L;
        return stats;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
