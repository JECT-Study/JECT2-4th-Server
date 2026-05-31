-- vote 테이블에서 "type" 컬럼 제거
-- GENERAL / IMMERSIVE 투표 유형 구분을 제거하고 단일 Vote 모델로 통합
-- 모든 투표 API(/api/votes, /api/immersive-votes, /api/home/*)는 vote 테이블의 데이터를 직접 사용

-- "type" 컬럼에 의존하는 인덱스 먼저 삭제 (컬럼 삭제 전 필수)
DROP INDEX IF EXISTS idx_vote_type_end_at;

-- "type" 컬럼 삭제 (PostgreSQL 9.2+ 에서 IF EXISTS 지원)
ALTER TABLE vote DROP COLUMN IF EXISTS "type";
