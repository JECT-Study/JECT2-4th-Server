package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.port.in.NotificationQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationQueryUseCase {

    private static final Duration ONE_MONTH = Duration.ofDays(30);

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Override
    public NotificationPageResult getList(Long userId, Long cursor, int size) {
        Instant oneMonthAgo = Instant.now(clock).minus(ONE_MONTH);
        Slice<Notification> slice = notificationRepository.findPage(
                userId, cursor, oneMonthAgo, PageRequest.ofSize(size));

        List<NotificationView> views = slice.getContent().stream()
                .map(NotificationView::from).toList();
        Long nextCursor = slice.hasNext() && !views.isEmpty()
                ? views.get(views.size() - 1).notificationId() : null;
        return new NotificationPageResult(views, nextCursor, slice.hasNext());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
