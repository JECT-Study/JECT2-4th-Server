package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException() {
        super(VoteErrorCode.UNAUTHORIZED);
    }
}
