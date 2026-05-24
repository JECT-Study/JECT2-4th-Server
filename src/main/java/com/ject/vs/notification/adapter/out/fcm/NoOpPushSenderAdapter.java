package com.ject.vs.notification.adapter.out.fcm;

import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("'${firebase.service-account-path:}' == ''")
@Slf4j
public class NoOpPushSenderAdapter implements PushSenderPort {

    @Override
    public List<String> sendMulticast(List<String> tokens, FcmPayload payload) {
        log.debug("FCM is disabled; skipping push notification payload={}", payload.type());
        return List.of();
    }
}
