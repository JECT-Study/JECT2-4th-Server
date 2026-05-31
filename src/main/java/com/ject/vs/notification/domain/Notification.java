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

    private boolean sent;
    private Instant sentAt;

    public static Notification ofVoteEnded(
            Long userId, Long voteId, String voteTitle, String thumbnailUrl, Clock clock) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = NotificationType.VOTE_ENDED;
        n.voteId = voteId;
        n.title = "투표 결과가 공개됐어요";
        n.body = "[" + voteTitle + "] 결과 보러가기";
        n.thumbnailUrl = thumbnailUrl;
        n.isRead = false;
        n.createdAt = Instant.now(clock);
        n.sent = false;
        return n;
    }

    /**
     * QA/테스트용 알림 생성 (커스텀 title, body 지정 가능)
     */
    public static Notification ofCustom(
            Long userId, Long voteId, String title, String body, String thumbnailUrl, Clock clock) {
        Notification n = new Notification();
        n.userId = userId;
        n.type = NotificationType.VOTE_ENDED;
        n.voteId = voteId;
        n.title = title;
        n.body = body;
        n.thumbnailUrl = thumbnailUrl;
        n.isRead = false;
        n.createdAt = Instant.now(clock);
        n.sent = false;
        return n;
    }

    public void markRead(Clock clock) {
        if (this.isRead) return;
        this.isRead = true;
        this.readAt = Instant.now(clock);
    }

    public void markSent(Clock clock) {
        if (this.sent) return;
        this.sent = true;
        this.sentAt = Instant.now(clock);
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
