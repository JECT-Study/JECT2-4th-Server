package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class InvalidEmojiException extends BusinessException {
    public InvalidEmojiException() {
        super(VoteErrorCode.INVALID_EMOJI);
    }
}
