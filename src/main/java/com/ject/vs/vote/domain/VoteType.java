package com.ject.vs.vote.domain;

public enum VoteType {
    GENERAL,
    IMMERSIVE;

    public static VoteType from(Vote vote) {
        return vote.getImageUrl() != null ? IMMERSIVE : GENERAL;
    }
}