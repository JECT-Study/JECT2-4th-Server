package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.InvalidDurationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum VoteDuration {
    MINUTES_7(Duration.ofMinutes(7)),
    MINUTES_10(Duration.ofMinutes(10)),
    MINUTES_15(Duration.ofMinutes(15)),
    HOURS_12(Duration.ofHours(12)),
    HOURS_24(Duration.ofHours(24)),
    HOURS_72(Duration.ofHours(72));

    private final Duration value;

    public static VoteDuration fromHours(int hours) {
        return Arrays.stream(values())
                .filter(d -> d.value.toHours() == hours)
                .findFirst()
                .orElseThrow(InvalidDurationException::new);
    }
}
