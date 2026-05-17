package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseTimeEntity;
import com.ject.vs.vote.exception.ImageRequiredException;
import com.ject.vs.vote.exception.VoteOptionNotFoundException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    @Column(nullable = false)
    private Instant endAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", insertable = false, updatable = false)
    private List<VoteOption> options = new ArrayList<>();

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
        vote.endAt = Instant.now(clock).plus(validityPeriod);
        return vote;
    }

    /**
     * 진실의 원천: endAt만 본다. status 컬럼은 보지 않는다.
     */
    public boolean isOngoing(Clock clock) {
        return Instant.now(clock).isBefore(endAt);
    }

    public boolean isEnded(Clock clock) {
        return !isOngoing(clock);
    }

    public VoteStatus getStatus(Clock clock) {
        return isOngoing(clock) ? VoteStatus.ONGOING : VoteStatus.ENDED;
    }

    public VoteStatus getStatus() {
        return getStatus(Clock.systemUTC());
    }

    public VoteOption getOptionA() {
        return getOption(VoteOptionCode.A)
                .orElseThrow(VoteOptionNotFoundException::new);
    }

    public VoteOption getOptionB() {
        return getOption(VoteOptionCode.B)
                .orElseThrow(VoteOptionNotFoundException::new);
    }

    public VoteOption getOption(Long optionId) {
        return orderedOptions().stream()
                .filter(opt -> opt.isIdEqualTo(optionId))
                .findFirst()
                .orElseThrow(VoteOptionNotFoundException::new);
    }

    private Optional<VoteOption> getOption(VoteOptionCode code) {
        return orderedOptions().stream()
                .filter(opt -> opt.isCodeEqualTo(code))
                .findFirst();
    }

    private Optional<String> getOptionLabel(int index) {
        List<VoteOption> orderedOptions = orderedOptions();
        if (orderedOptions.size() <= index) {
            return Optional.empty();
        }
        return Optional.ofNullable(orderedOptions.get(index).getLabel());
    }

    private List<VoteOption> orderedOptions() {
        return options.stream()
                .sorted(Comparator.comparingInt(VoteOption::getPosition))
                .toList();
    }

    public void cacheAiInsight(String headline, String body) {
        this.aiInsightHeadline = headline;
        this.aiInsightBody = body;
    }

    public boolean hasAiInsight() {
        return aiInsightHeadline != null && aiInsightBody != null;
    }
}
