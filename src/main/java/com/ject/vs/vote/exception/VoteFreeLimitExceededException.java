package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class VoteFreeLimitExceededException extends BusinessException {
    public VoteFreeLimitExceededException() {
        super(VoteErrorCode.VOTE_FREE_LIMIT_EXCEEDED);
    }
}
