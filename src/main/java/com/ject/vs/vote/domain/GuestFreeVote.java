package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Clock;
import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "guest_free_vote")
@Getter
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GuestFreeVote {

    private static final int MAX_FREE_VOTES = 5;

    @Id
    private String anonymousId;

    private int consumedCount;
    private Instant lastConsumedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public static GuestFreeVote create(String anonymousId) {
        GuestFreeVote g = new GuestFreeVote();
        g.anonymousId = anonymousId;
        g.consumedCount = 0;
        return g;
    }

    public void consume(Clock clock) {
        if (consumedCount >= MAX_FREE_VOTES) {
            throw new VoteFreeLimitExceededException();
        }
        consumedCount++;
        lastConsumedAt = Instant.now(clock);
    }

    public int remaining() {
        return MAX_FREE_VOTES - consumedCount;
    }

    public static int totalFreeVotes() {
        return MAX_FREE_VOTES;
    }
}
