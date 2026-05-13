package com.ject.vs.notification.exception;

import com.ject.vs.common.exception.BusinessException;

public class PushSubscriptionInvalidException extends BusinessException {
    public PushSubscriptionInvalidException() {
        super(NotificationErrorCode.PUSH_SUBSCRIPTION_INVALID);
    }
}
