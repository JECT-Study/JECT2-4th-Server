package com.ject.vs.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByUserIdAndToken(Long userId, String token);

    List<PushToken> findAllByUserId(Long userId);

    List<PushToken> findAllByUserIdIn(Collection<Long> userIds);

    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM PushToken t WHERE t.token IN :tokens")
    void deleteAllByTokenIn(Collection<String> tokens);
}
