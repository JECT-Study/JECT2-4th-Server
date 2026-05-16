package com.ject.vs.notification.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n
         WHERE n.userId = :userId
           AND n.createdAt >= :oneMonthAgo
           AND (:cursor IS NULL OR n.id < :cursor)
         ORDER BY n.id DESC
        """)
    Slice<Notification> findPage(@Param("userId") Long userId,
                                 @Param("cursor") Long cursor,
                                 @Param("oneMonthAgo") Instant oneMonthAgo,
                                 Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("""
        UPDATE Notification n
           SET n.isRead = true, n.readAt = :now
         WHERE n.userId = :userId AND n.isRead = false
        """)
    int markAllAsRead(@Param("userId") Long userId, @Param("now") Instant now);
}
