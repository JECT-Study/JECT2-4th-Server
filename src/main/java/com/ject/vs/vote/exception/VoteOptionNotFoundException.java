package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class VoteOptionNotFoundException extends BusinessException {
    public VoteOptionNotFoundException() {
        super(VoteErrorCode.VOTE_OPTION_NOT_FOUND);
    }
}
