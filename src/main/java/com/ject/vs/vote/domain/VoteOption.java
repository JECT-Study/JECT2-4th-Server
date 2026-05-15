package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "vote_option")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VoteOption extends BaseEntity {

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;

    public static VoteOption of(Long voteId, String label, int position) {
        VoteOption option = new VoteOption();
        option.voteId = voteId;
        option.label = label;
        option.position = position;
        return option;
    }
}
