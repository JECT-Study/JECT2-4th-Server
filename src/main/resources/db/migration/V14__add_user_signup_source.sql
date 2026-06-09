-- 가입 출처(UTM) 컬럼 추가.
-- 비회원이 UTM 링크로 유입(/api/track/visit)되면 백엔드가 first-touch UTM을 쿠키에 박제하고,
-- 소셜 로그인 가입이 "처음 완료되는 순간"(users row 최초 생성 시)에만 아래 컬럼에 기록한다.
-- 기존 사용자 재로그인 시에는 덮어쓰지 않는다(first-touch 고정).
ALTER TABLE users ADD COLUMN signup_source   VARCHAR(255);
ALTER TABLE users ADD COLUMN signup_medium   VARCHAR(255);
ALTER TABLE users ADD COLUMN signup_campaign VARCHAR(255);
ALTER TABLE users ADD COLUMN signup_content  VARCHAR(255);
