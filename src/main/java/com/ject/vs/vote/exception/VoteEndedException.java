package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class VoteEndedException extends BusinessException {
    public VoteEndedException() {
        super(VoteErrorCode.VOTE_ENDED);
    }
}
