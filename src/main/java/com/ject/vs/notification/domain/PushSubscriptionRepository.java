package com.ject.vs.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByUserId(Long userId);
    List<PushSubscription> findAllByUserIdIn(Collection<Long> userIds);
    void deleteAllByUserId(Long userId);
    void deleteByEndpoint(String endpoint);
}
