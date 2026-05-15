package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class InvalidOptionException extends BusinessException {
    public InvalidOptionException() {
        super(VoteErrorCode.INVALID_OPTION);
    }
}
