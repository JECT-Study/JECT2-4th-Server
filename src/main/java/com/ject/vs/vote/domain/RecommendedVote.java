package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "recommended_vote")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RecommendedVote extends BaseTimeEntity {

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private LocalDate recommendedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voteId", insertable = false, updatable = false)
    private Vote vote;

    public static RecommendedVote create(Long voteId, Integer displayOrder, LocalDate recommendedDate) {
        RecommendedVote recommended = new RecommendedVote();
        recommended.voteId = voteId;
        recommended.displayOrder = displayOrder;
        recommended.recommendedDate = recommendedDate;
        return recommended;
    }
}
