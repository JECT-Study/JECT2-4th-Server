-- 투표 종료 후속 처리(알림 등)를 한 번만 트리거하기 위한 마커
ALTER TABLE vote ADD COLUMN ended_processed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_vote_unprocessed_expired
    ON vote (end_at)
    WHERE ended_processed_at IS NULL;

-- 배포 시점에 이미 종료된 투표는 재처리하지 않음
UPDATE vote
   SET ended_processed_at = end_at
 WHERE end_at < now()
   AND ended_processed_at IS NULL;

-- 동일 투표·사용자·타입 알림 중복 방지 (race condition 안전망)
DELETE FROM notification n1
 USING notification n2
 WHERE n1.id > n2.id
   AND n1.vote_id IS NOT NULL
   AND n1.vote_id = n2.vote_id
   AND n1.user_id = n2.user_id
   AND n1.type = n2.type;

CREATE UNIQUE INDEX uq_notification_vote_user_type
    ON notification (vote_id, user_id, type)
    WHERE vote_id IS NOT NULL;