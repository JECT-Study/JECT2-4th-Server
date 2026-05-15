package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.InvalidDurationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum VoteDuration {
    HOURS_12(Duration.ofHours(12)),
    HOURS_24(Duration.ofHours(24));

    private final Duration value;

    public static VoteDuration fromHours(int hours) {
        return Arrays.stream(values())
                .filter(d -> d.value.toHours() == hours)
                .findFirst()
                .orElseThrow(InvalidDurationException::new);
    }
}
