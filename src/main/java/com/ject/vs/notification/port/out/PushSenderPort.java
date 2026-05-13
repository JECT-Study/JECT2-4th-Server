package com.ject.vs.notification.port.out;

import com.ject.vs.notification.domain.PushSubscription;

public interface PushSenderPort {

    SendResult send(PushSubscription subscription, PushPayload payload);

    enum SendResult {
        OK, GONE, RETRYABLE_ERROR
    }
}
