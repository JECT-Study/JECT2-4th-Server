-- =============================================================================
--  QA 더미 데이터 정리(삭제) 스크립트  — scripts/qa-seed.sql 짝꿍
-- =============================================================================
--  qa-seed.sql 로 심은 데이터만 정확히 회수한다.
--  앵커(식별 기준):
--    - 더미 투표 : qa_seed_vote 추적 테이블에 기록된 id
--    - 더미 유저 : users.sub LIKE 'qa-dummy-%'
--    - 더미 게스트: guest_free_vote.anonymous_id LIKE 'qa-guest-%'
--
--  FK 제약 때문에 자식 → 부모 순서로 삭제한다.
--  한 트랜잭션으로 실행 → 중간 에러 시 전체 롤백.
-- =============================================================================

BEGIN;

-- 추적 테이블이 없으면(이미 정리됨) 빈 결과로 동작하도록 보장
CREATE TABLE IF NOT EXISTS qa_seed_vote (id BIGINT PRIMARY KEY, seq INT, kind TEXT);

-- 1) 채팅 읽음 상태
DELETE FROM chat_room_unread
WHERE vote_id IN (SELECT id FROM qa_seed_vote)
   OR user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 2) 채팅 메시지
DELETE FROM chat_message
WHERE vote_id IN (SELECT id FROM qa_seed_vote)
   OR sender_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 3) 이모지 반응
DELETE FROM vote_emoji_reaction
WHERE vote_id IN (SELECT id FROM qa_seed_vote)
   OR user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 4) 알림 (vote_id FK → vote 이므로 vote 삭제 전에)
DELETE FROM notification
WHERE vote_id IN (SELECT id FROM qa_seed_vote)
   OR user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 5) 투표 참여 (option_id FK → vote_option 이므로 option 삭제 전에)
DELETE FROM vote_participation
WHERE vote_id IN (SELECT id FROM qa_seed_vote)
   OR user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 6) 오늘의 추천
DELETE FROM recommended_vote
WHERE vote_id IN (SELECT id FROM qa_seed_vote);

-- 7) 조회수 통계
DELETE FROM vote_statistics
WHERE vote_id IN (SELECT id FROM qa_seed_vote);

-- 8) 선택지
DELETE FROM vote_option
WHERE vote_id IN (SELECT id FROM qa_seed_vote);

-- 9) 투표 본체
DELETE FROM vote
WHERE id IN (SELECT id FROM qa_seed_vote);

-- 10) 더미 유저 부속 데이터 (혹시 생성됐을 경우 대비)
DELETE FROM push_subscription WHERE user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');
DELETE FROM push_token        WHERE user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');
DELETE FROM token             WHERE user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');
DELETE FROM notification_setting WHERE user_id IN (SELECT id FROM users WHERE sub LIKE 'qa-dummy-%');

-- 11) 더미 유저
DELETE FROM users WHERE sub LIKE 'qa-dummy-%';

-- 12) 더미 게스트
DELETE FROM guest_free_vote WHERE anonymous_id LIKE 'qa-guest-%';

-- 13) 추적 테이블 제거
DROP TABLE IF EXISTS qa_seed_vote;

COMMIT;

-- =============================================================================
--  주의: 테스트(본인) 계정 자체는 삭제하지 않는다. 본인 계정이 더미 투표에
--        남긴 참여/알림/채팅은 위 vote_id 기준 삭제로 함께 회수된다.
-- =============================================================================
