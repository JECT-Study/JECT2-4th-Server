package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import com.ject.vs.vote.exception.ImageRequiredException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "vote")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Vote extends BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType type;

    @Column(nullable = false)
    private String title;

    private String content;

    @Column(nullable = false)
    private String thumbnailUrl;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteStatus status;

    @Column(nullable = false)
    private Instant endAt;

    private String aiInsightHeadline;
    private String aiInsightBody;

    public static Vote create(VoteType type, String title, String content,
                              String thumbnailUrl, String imageUrl,
                              Duration validityPeriod, Clock clock) {
        if (type == VoteType.IMMERSIVE && (imageUrl == null || imageUrl.isBlank())) {
            throw new ImageRequiredException();
        }
        Vote vote = new Vote();
        vote.type = type;
        vote.title = title;
        vote.content = content;
        vote.thumbnailUrl = thumbnailUrl;
        vote.imageUrl = imageUrl;
        vote.status = VoteStatus.ONGOING;
        vote.endAt = Instant.now(clock).plus(validityPeriod);
        return vote;
    }

    /** 진실의 원천: endAt만 본다. status 컬럼은 보지 않는다. */
    public boolean isOngoing(Clock clock) {
        return Instant.now(clock).isBefore(endAt);
    }

    public boolean isEnded(Clock clock) {
        return !isOngoing(clock);
    }

    /** 스케줄러용 — status 컬럼을 ENDED로 마킹 (캐시 갱신) */
    public void markEnded() {
        this.status = VoteStatus.ENDED;
    }

    public void cacheAiInsight(String headline, String body) {
        this.aiInsightHeadline = headline;
        this.aiInsightBody = body;
    }

    public boolean hasAiInsight() {
        return aiInsightHeadline != null && aiInsightBody != null;
    }
}
