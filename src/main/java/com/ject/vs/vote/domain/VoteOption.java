package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "vote_option")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteOption extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "vote_id", nullable = false, updatable = false)
    private Vote vote;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;

    public static VoteOption of(Vote vote, String label, int position) {
        VoteOption option = new VoteOption();
        option.vote = vote;
        option.label = label;
        option.position = position;
        return option;
    }

    public VoteOptionCode getCode() {
        return position == 0 ? VoteOptionCode.A : VoteOptionCode.B;
    }

    public Boolean isCodeEqualTo(VoteOptionCode code) {
        return getCode().equals(code);
    }
}
