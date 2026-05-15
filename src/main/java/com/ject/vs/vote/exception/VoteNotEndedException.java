package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class VoteNotEndedException extends BusinessException {
    public VoteNotEndedException() {
        super(VoteErrorCode.VOTE_NOT_ENDED);
    }
}
