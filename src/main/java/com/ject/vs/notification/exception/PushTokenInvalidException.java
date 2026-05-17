package com.ject.vs.notification.exception;

import com.ject.vs.common.exception.BusinessException;

public class PushTokenInvalidException extends BusinessException {
    public PushTokenInvalidException() {
        super(NotificationErrorCode.PUSH_TOKEN_INVALID);
    }
}
