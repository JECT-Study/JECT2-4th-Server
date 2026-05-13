package com.ject.vs.notification.adapter.out.webpush;

import com.ject.vs.notification.domain.PushSubscription;
import com.ject.vs.notification.port.out.PushPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebPushSenderAdapter implements PushSenderPort {

    private final PushService pushService;

    @Override
    public SendResult send(PushSubscription subscription, PushPayload payload) {
        try {
            byte[] authKey = Base64.getUrlDecoder().decode(subscription.getAuthKey());
            nl.martijndwars.webpush.Notification notif = new nl.martijndwars.webpush.Notification(
                    subscription.getEndpoint(),
                    Utils.loadPublicKey(subscription.getP256dhKey()),
                    authKey,
                    payload.toJson().getBytes(StandardCharsets.UTF_8)
            );
            HttpResponse response = pushService.send(notif);
            int status = response.getStatusLine().getStatusCode();
            if (status == 201 || status == 200) return SendResult.OK;
            if (status == 404 || status == 410) return SendResult.GONE;
            log.warn("Web Push unexpected status={} endpoint={}", status, subscription.getEndpoint());
            return SendResult.RETRYABLE_ERROR;
        } catch (Exception e) {
            log.error("Web Push send failed endpoint={}", subscription.getEndpoint(), e);
            return SendResult.RETRYABLE_ERROR;
        }
    }
}
