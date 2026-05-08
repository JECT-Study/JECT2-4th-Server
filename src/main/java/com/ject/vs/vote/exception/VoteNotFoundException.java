package com.ject.vs.vote.exception;

public class VoteNotFoundException extends RuntimeException {

    public VoteNotFoundException() {
        super("투표를 찾을 수 없습니다.");
    }

    public String getErrorCode() {
        return "VOTE_NOT_FOUND";
    }
}
