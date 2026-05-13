package com.ject.vs.notification.domain;

import com.ject.vs.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    private Long userId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long voteId;
    private String title;
    private String body;
    private String thumbnailUrl;

    private boolean isRead;
    private Instant readAt;
    private Instant createdAt;

    public static Notification ofVoteResultPublished(
            Long userId, Long voteId, String voteTitle, String thumbnailUrl, Clock clock) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = NotificationType.VOTE_RESULT_PUBLISHED;
        n.voteId = voteId;
        n.title = voteTitle;
        n.body = "투표 결과가 공개됐어요";
        n.thumbnailUrl = thumbnailUrl;
        n.isRead = false;
        n.createdAt = Instant.now(clock);
        return n;
    }

    public void markRead(Clock clock) {
        if (this.isRead) return;
        this.isRead = true;
        this.readAt = Instant.now(clock);
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
