package com.ject.vs.notification.port.in;

public interface PushSubscriptionUseCase {
    Long register(Long userId, String endpoint, String p256dhKey,
                  String authKey, String userAgent);
    void unregisterAll(Long userId);
}
