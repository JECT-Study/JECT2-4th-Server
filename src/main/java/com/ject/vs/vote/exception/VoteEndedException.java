package com.ject.vs.vote.exception;

public class VoteEndedException extends RuntimeException {

    public VoteEndedException() {
        super("이미 종료된 투표입니다.");
    }

    public String getErrorCode() {
        return "VOTE_ENDED";
    }
}
