package com.ject.vs.vote.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum VoteEmoji {
    LIKE, SAD, ANGRY, WOW;

    public static Map<VoteEmoji, Long> getMap() {
        return Arrays.stream(VoteEmoji.values())
                .collect(Collectors.toMap(e -> e, e -> 0L));
    }
}
