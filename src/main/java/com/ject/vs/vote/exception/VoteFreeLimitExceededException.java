package com.ject.vs.vote.exception;

public class VoteFreeLimitExceededException extends RuntimeException {

    public VoteFreeLimitExceededException() {
        super("무료 투표 횟수를 초과했습니다.");
    }

    public String getErrorCode() {
        return "VOTE_FREE_LIMIT_EXCEEDED";
    }
}
