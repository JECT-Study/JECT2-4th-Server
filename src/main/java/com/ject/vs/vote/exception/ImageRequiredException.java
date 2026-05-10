package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.BusinessException;

public class ImageRequiredException extends BusinessException {
    public ImageRequiredException() {
        super(VoteErrorCode.IMAGE_REQUIRED);
    }
}
