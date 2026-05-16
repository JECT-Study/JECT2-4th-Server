package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.Platform;
import com.ject.vs.notification.domain.PushToken;
import com.ject.vs.notification.domain.PushTokenRepository;
import com.ject.vs.notification.port.in.PushTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class PushTokenService implements PushTokenUseCase {

    private final PushTokenRepository repository;
    private final Clock clock;

    @Override
    public void register(Long userId, String token, Platform platform) {
        repository.findByUserIdAndToken(userId, token)
                .ifPresentOrElse(
                        t -> t.touch(clock),
                        () -> repository.save(PushToken.of(userId, token, platform, clock)));
    }

    @Override
    public void unregisterAll(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
