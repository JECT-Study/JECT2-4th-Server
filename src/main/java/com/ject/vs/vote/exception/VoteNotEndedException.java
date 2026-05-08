package com.ject.vs.vote.exception;

public class VoteNotEndedException extends RuntimeException {

    public VoteNotEndedException() {
        super("아직 진행 중인 투표입니다.");
    }

    public String getErrorCode() {
        return "VOTE_NOT_ENDED";
    }
}
