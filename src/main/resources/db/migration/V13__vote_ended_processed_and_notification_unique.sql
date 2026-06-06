-- 투표 종료 후속 처리(알림 등)를 한 번만 트리거하기 위한 마커
ALTER TABLE vote ADD COLUMN ended_processed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_vote_ended_processed_end_at ON vote (ended_processed_at, end_at);

-- 배포 시점에 이미 종료된 투표는 재처리하지 않음
UPDATE vote
   SET ended_processed_at = end_at
 WHERE end_at < CURRENT_TIMESTAMP
   AND ended_processed_at IS NULL;