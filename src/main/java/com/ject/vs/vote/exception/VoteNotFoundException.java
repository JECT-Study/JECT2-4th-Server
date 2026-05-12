package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class VoteNotFoundException extends BusinessException {
    public VoteNotFoundException() {
        super(VoteErrorCode.VOTE_NOT_FOUND);
    }
}
