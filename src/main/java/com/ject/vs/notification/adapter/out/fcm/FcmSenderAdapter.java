package com.ject.vs.notification.adapter.out.fcm;

import com.google.firebase.messaging.*;
import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmSenderAdapter implements PushSenderPort {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public List<String> sendMulticast(List<String> tokens, FcmPayload payload) {
        if (tokens.isEmpty()) return List.of();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build())
                .putAllData(payload.toDataMap())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.debug("FCM multicast successCount={} failureCount={}",
                    response.getSuccessCount(), response.getFailureCount());
            return collectExpiredTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast failed payload={}", payload.type(), e);
            return List.of();
        }
    }

    private List<String> collectExpiredTokens(List<String> tokens, BatchResponse response) {
        List<String> expired = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (!r.isSuccessful()) {
                MessagingErrorCode code = r.getException().getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED
                        || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    expired.add(tokens.get(i));
                } else {
                    log.warn("FCM send failed token={} code={}", tokens.get(i), code);
                }
            }
        }
        return expired;
    }
}
