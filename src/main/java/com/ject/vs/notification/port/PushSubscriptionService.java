package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.PushSubscription;
import com.ject.vs.notification.domain.PushSubscriptionRepository;
import com.ject.vs.notification.port.in.PushSubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PushSubscriptionService implements PushSubscriptionUseCase {

    private final PushSubscriptionRepository repository;
    private final Clock clock;

    @Override
    public Long register(Long userId, String endpoint, String p256dhKey,
                         String authKey, String userAgent) {
        // 동일 endpoint가 이미 있으면 last_used_at만 갱신 (upsert)
        Optional<PushSubscription> existing = repository.findByEndpoint(endpoint);
        if (existing.isPresent()) {
            PushSubscription p = existing.get();
            p.touch(clock);
            return p.getId();
        }
        PushSubscription saved = repository.save(
                PushSubscription.of(userId, endpoint, p256dhKey, authKey, userAgent, clock));
        return saved.getId();
    }

    @Override
    public void unregisterAll(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
