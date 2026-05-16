-- vote_option.vote_id → vote(id) 외래키 제약조건 명시적 추가
-- V3에서 CREATE TABLE 시 inline REFERENCES로 unnamed FK(vote_option_vote_id_fkey)가 이미 생성됨
-- 기존 unnamed 제약을 제거하고, 프로젝트 네이밍 컨벤션(fk_<table>_<ref_table>)에 맞춰 재생성

ALTER TABLE vote_option
    DROP CONSTRAINT IF EXISTS vote_option_vote_id_fkey;

ALTER TABLE vote_option
    ADD CONSTRAINT fk_vote_option_vote
    FOREIGN KEY (vote_id) REFERENCES vote (id);
