package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class InvalidDurationException extends BusinessException {
    public InvalidDurationException() {
        super(VoteErrorCode.INVALID_DURATION);
    }
}
