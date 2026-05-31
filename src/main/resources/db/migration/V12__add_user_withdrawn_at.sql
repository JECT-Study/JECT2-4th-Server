-- 회원 soft delete 도입: 탈퇴 시각 컬럼 추가
-- user_status = 'WITHDRAWN' 과 함께 30일 재가입 제한 판정에 사용한다.
ALTER TABLE users ADD COLUMN withdrawn_at TIMESTAMP;
