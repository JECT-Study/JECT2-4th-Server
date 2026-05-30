-- =============================================================================
--  QA 더미 데이터 시드 스크립트  (운영 DB / PostgreSQL · Neon)
-- =============================================================================
--  실행 전 필수:
--   1) 아래 __MY_USER_ID__ 를 본인 user id 숫자로 전부 치환 (찾는 법: 파일 하단 주석)
--   2) 가능하면 Neon에서 브랜치/스냅샷 한 번 떠두고 실행
--   3) 한 트랜잭션으로 실행됨 → 중간 에러 시 전체 롤백
--
--  정리(삭제)는 짝꿍 스크립트 scripts/qa-cleanup.sql 로 수행.
--  심는 데이터는 모두 식별자가 박혀 있어 안전하게 회수 가능:
--    - 더미 유저 : users.sub LIKE 'qa-dummy-%'
--    - 더미 투표 : qa_seed_vote 추적 테이블에 id 기록
--    - 더미 게스트: guest_free_vote.anonymous_id LIKE 'qa-guest-%'
-- =============================================================================

BEGIN;

-- 진행/종료는 status 컬럼이 아니라 end_at vs now() 로만 판정됨(코드 확인). end_at만 잘 잡으면 됨.

-- 0) 더미 투표 id 추적 테이블 -------------------------------------------------
CREATE TABLE IF NOT EXISTS qa_seed_vote (
    id   BIGINT PRIMARY KEY,
    seq  INT,
    kind TEXT          -- 'ongoing' | 'ended'
);

-- 1) 더미 유저 60명 (성별/연령 다양) -----------------------------------------
INSERT INTO users (sub, nickname, profile_icon_url, birth_year, gender, image_color, email, user_status)
SELECT
    'qa-dummy-' || g,
    'QA유저' || lpad(g::text, 2, '0'),
    NULL,
    1975 + (g % 35),                                              -- 1975 ~ 2009 분포
    CASE WHEN g % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    (ARRAY['GREEN','RED','BLUE','YELLOW'])[1 + (g % 4)],
    'qa-dummy-' || g || '@example.com',
    'REGISTER'
FROM generate_series(1, 60) AS g;

-- 더미 유저 알림설정 row (push 기본 off)
INSERT INTO notification_setting (user_id)
SELECT id FROM users WHERE sub LIKE 'qa-dummy-%'
ON CONFLICT (user_id) DO NOTHING;

-- 2) 진행 중 투표 50개 --------------------------------------------------------
--    seq 1  : 종료 임박 1h
--    seq 2  : 종료 임박 12h
--    seq 10,11,12 : 참여자 최다(60명)+조회수 높음 → 핫토픽 TOP3 후보
--    seq 49 : 참여자 0명
--    seq 5,6,7 : 오늘의 추천 대상
WITH new_votes AS (
    INSERT INTO vote (type, title, content, thumbnail_url, image_url, status, end_at, created_at, updated_at, ai_insight_headline, ai_insight_body)
    SELECT
        'GENERAL',
        'QA 진행중 투표 #' || g,
        '진행 중 투표 본문입니다. 번호 ' || g,
        'https://picsum.photos/seed/qa-on-' || g || '/400/300',
        NULL,
        'ONGOING',
        CASE
            WHEN g = 1 THEN now() + interval '1 hour'      -- 종료 임박 1h
            WHEN g = 2 THEN now() + interval '12 hours'    -- 종료 임박 12h
            ELSE now() + (((g % 10) + 1) || ' days')::interval
        END,
        now() - ((g % 48 + 1) || ' hours')::interval,
        now(),
        NULL, NULL
    FROM generate_series(1, 50) AS g
    RETURNING id, title
)
INSERT INTO qa_seed_vote (id, seq, kind)
SELECT id, split_part(title, '#', 2)::int, 'ongoing' FROM new_votes;

-- 3) 종료된 투표 7개 ----------------------------------------------------------
--    seq 1 : 테스트계정 참여O + 채팅 5건
--    seq 2 : 테스트계정 참여O + 채팅 300건 초과
--    seq 3 : 테스트계정 참여X (다른 유저는 참여)
--    seq 4 : 성별/연령 분포 다양 (도넛·막대 차트용, 50명 참여)
--    seq 5 : AI 인사이트 생성 완료
--    seq 6 : 어제 종료
--    seq 7 : 일주일 전 종료
WITH new_votes AS (
    INSERT INTO vote (type, title, content, thumbnail_url, image_url, status, end_at, created_at, updated_at, ai_insight_headline, ai_insight_body)
    SELECT
        'GENERAL',
        'QA 종료 투표 #' || g,
        '종료된 투표 본문입니다. 번호 ' || g,
        'https://picsum.photos/seed/qa-end-' || g || '/400/300',
        NULL,
        'ENDED',
        CASE
            WHEN g = 6 THEN now() - interval '1 day'       -- 어제 종료
            WHEN g = 7 THEN now() - interval '7 days'      -- 일주일 전 종료
            ELSE now() - ((g + 1) || ' days')::interval    -- 2~6일 전
        END,
        now() - interval '14 days',
        now(),
        CASE WHEN g = 5 THEN '응답자의 68%가 찬성했어요' ELSE NULL END,
        CASE WHEN g = 5 THEN '20대 여성층의 찬성 비율이 특히 높았고, 연령이 높아질수록 반대 의견이 늘어나는 경향을 보였습니다. 전체적으로 활발한 참여가 이루어진 투표였어요.' ELSE NULL END
    FROM generate_series(1, 7) AS g
    RETURNING id, title
)
INSERT INTO qa_seed_vote (id, seq, kind)
SELECT id, split_part(title, '#', 2)::int, 'ended' FROM new_votes;

-- 4) 모든 투표에 선택지 2개 ---------------------------------------------------
INSERT INTO vote_option (vote_id, label, position)
SELECT v.id, opt.label, opt.position
FROM qa_seed_vote v
CROSS JOIN (VALUES ('찬성', 0), ('반대', 1)) AS opt(label, position);

-- 5) 더미 유저 참여 (진행중) --------------------------------------------------
INSERT INTO vote_participation (vote_id, user_id, anonymous_id, option_id, created_at, updated_at)
SELECT
    v.id,
    u.id,
    NULL,
    (SELECT o.id FROM vote_option o WHERE o.vote_id = v.id ORDER BY o.position LIMIT 1 OFFSET (u.rn % 2)),
    now() - (u.rn || ' minutes')::interval,
    now()
FROM qa_seed_vote v
JOIN (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users WHERE sub LIKE 'qa-dummy-%'
) u
  ON u.rn <= CASE
                WHEN v.seq IN (10, 11, 12) THEN 60     -- 핫토픽 후보: 최다
                WHEN v.seq = 49            THEN 0      -- 참여자 0명
                ELSE (v.seq % 25) + 3                  -- 3 ~ 27명 다양
             END
WHERE v.kind = 'ongoing';

-- 6) 더미 유저 참여 (종료) ----------------------------------------------------
INSERT INTO vote_participation (vote_id, user_id, anonymous_id, option_id, created_at, updated_at)
SELECT
    v.id,
    u.id,
    NULL,
    (SELECT o.id FROM vote_option o WHERE o.vote_id = v.id ORDER BY o.position LIMIT 1 OFFSET (u.rn % 2)),
    now() - interval '8 days' - (u.rn || ' minutes')::interval,
    now()
FROM qa_seed_vote v
JOIN (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users WHERE sub LIKE 'qa-dummy-%'
) u
  ON u.rn <= CASE
                WHEN v.seq = 4 THEN 50                 -- 성별/연령 차트용 50명
                ELSE 20                                -- 결과 페이지용 기본 20명
             END
WHERE v.kind = 'ended';

-- 7) 테스트(본인) 계정 참여 --------------------------------------------------
--    진행중 일부 + 종료 seq 1,2,4,5,6,7 (seq 3 은 일부러 미참여)
INSERT INTO vote_participation (vote_id, user_id, anonymous_id, option_id, created_at, updated_at)
SELECT
    v.id,
    __MY_USER_ID__,
    NULL,
    (SELECT o.id FROM vote_option o WHERE o.vote_id = v.id ORDER BY o.position LIMIT 1),
    v_created.ts,
    now()
FROM qa_seed_vote v
CROSS JOIN LATERAL (SELECT now() - interval '6 days' AS ts) v_created
WHERE (v.kind = 'ended'   AND v.seq IN (1, 2, 4, 5, 6, 7))
   OR (v.kind = 'ongoing' AND v.seq IN (3, 11, 20));

-- 8) 채팅 메시지 -------------------------------------------------------------
-- 8-1) 진행중 seq 10 : 채팅 활발 (35건)
INSERT INTO chat_message (vote_id, sender_id, content, created_at, updated_at)
SELECT
    v.id,
    (SELECT id FROM users WHERE sub = 'qa-dummy-' || (1 + (n % 60))),
    'QA 실시간 채팅 메시지 ' || n,
    now() - (n || ' minutes')::interval,
    now()
FROM qa_seed_vote v
CROSS JOIN generate_series(1, 35) AS n
WHERE v.kind = 'ongoing' AND v.seq = 10;

-- 8-2) 종료 seq 1 : 채팅 5건
INSERT INTO chat_message (vote_id, sender_id, content, created_at, updated_at)
SELECT
    v.id,
    (SELECT id FROM users WHERE sub = 'qa-dummy-' || (1 + (n % 60))),
    'QA 종료투표 채팅 ' || n,
    now() - interval '8 days' + (n || ' minutes')::interval,
    now()
FROM qa_seed_vote v
CROSS JOIN generate_series(1, 5) AS n
WHERE v.kind = 'ended' AND v.seq = 1;

-- 8-3) 종료 seq 2 : 채팅 320건 (300+ 뱃지)
INSERT INTO chat_message (vote_id, sender_id, content, created_at, updated_at)
SELECT
    v.id,
    (SELECT id FROM users WHERE sub = 'qa-dummy-' || (1 + (n % 60))),
    'QA 대량 채팅 메시지 ' || n,
    now() - interval '9 days' + (n || ' seconds')::interval,
    now()
FROM qa_seed_vote v
CROSS JOIN generate_series(1, 320) AS n
WHERE v.kind = 'ended' AND v.seq = 2;

-- 9) 조회수 통계 (핫토픽 점수 = 참여*0.7 + 조회*0.3) --------------------------
INSERT INTO vote_statistics (vote_id, view_count)
SELECT
    v.id,
    CASE
        WHEN v.kind = 'ongoing' AND v.seq = 10 THEN 5000
        WHEN v.kind = 'ongoing' AND v.seq = 11 THEN 3500
        WHEN v.kind = 'ongoing' AND v.seq = 12 THEN 2500
        ELSE (v.seq * 37) % 800                          -- 그 외 잡다한 조회수
    END
FROM qa_seed_vote v;

-- 10) 오늘의 추천 3개 (진행중 투표만 / 운영진 선정 처리됨) ---------------------
INSERT INTO recommended_vote (vote_id, display_order, recommended_date, created_at, updated_at)
SELECT
    v.id,
    row_number() OVER (ORDER BY v.seq),
    CURRENT_DATE,
    now(),
    now()
FROM qa_seed_vote v
WHERE v.kind = 'ongoing' AND v.seq IN (5, 6, 7);

-- 11) 알림 3건 (테스트 계정, 모두 미읽음) ------------------------------------
INSERT INTO notification (user_id, type, vote_id, title, body, thumbnail_url, is_read, created_at, sent, sent_at)
VALUES
    (__MY_USER_ID__, 'VOTE_ENDED',
     (SELECT id FROM qa_seed_vote WHERE kind='ended' AND seq=6),
     '참여하신 투표가 종료되었어요', '결과를 확인해보세요! (24시간 이내 알림)',
     NULL, FALSE, now() - interval '3 hours',  TRUE, now() - interval '3 hours'),
    (__MY_USER_ID__, 'VOTE_ENDED',
     (SELECT id FROM qa_seed_vote WHERE kind='ended' AND seq=7),
     '참여하신 투표가 종료되었어요', '결과를 확인해보세요! (7일 이내 알림)',
     NULL, FALSE, now() - interval '3 days',   TRUE, now() - interval '3 days'),
    (__MY_USER_ID__, 'VOTE_ENDED',
     (SELECT id FROM qa_seed_vote WHERE kind='ended' AND seq=4),
     '참여하신 투표가 종료되었어요', '결과를 확인해보세요! (7일 초과 알림)',
     NULL, FALSE, now() - interval '10 days',  TRUE, now() - interval '10 days');

-- 12) 비회원 무료 투표 잔여 횟수 조정 (MAX=5) --------------------------------
--     remaining = 5 - consumed_count.  잔여 1회로 세팅하려면 consumed_count = 4.
INSERT INTO guest_free_vote (anonymous_id, consumed_count, last_consumed_at, created_at, updated_at)
VALUES
    ('qa-guest-1', 4, now() - interval '1 hour', now(), now()),   -- 잔여 1회
    ('qa-guest-2', 0, NULL,                      now(), now());   -- 잔여 5회(풀)
-- 운영 중 임의 조정 예시:  UPDATE guest_free_vote SET consumed_count = 4 WHERE anonymous_id = '<실제 anonymousId>';

-- 결과 요약 ------------------------------------------------------------------
SELECT 'dummy_users'  AS item, count(*) FROM users           WHERE sub LIKE 'qa-dummy-%'
UNION ALL SELECT 'dummy_votes',   count(*) FROM qa_seed_vote
UNION ALL SELECT 'participations', count(*) FROM vote_participation WHERE vote_id IN (SELECT id FROM qa_seed_vote)
UNION ALL SELECT 'chat_messages',  count(*) FROM chat_message       WHERE vote_id IN (SELECT id FROM qa_seed_vote)
UNION ALL SELECT 'notifications',  count(*) FROM notification        WHERE vote_id IN (SELECT id FROM qa_seed_vote);

COMMIT;

-- =============================================================================
--  본인 user id 찾는 법
--   방법1) SELECT id, email, nickname FROM users WHERE email = 'cjunk0304@gmail.com';
--   방법2) 액세스 토큰을 jwt.io 에 붙여넣고 sub 클레임 숫자 확인
--  → 찾은 숫자로 이 파일의 __MY_USER_ID__ 를 전부 치환 후 실행.
--
--  주의: '오늘의 추천 어드민 API' 자체를 테스트하려면 application.yml 의
--        admin.user-ids 목록(현재 [7])에 본인 id가 있어야 함. 위 시드는 추천
--        데이터를 DB에 직접 넣으므로 그 설정 없이도 추천 목록은 노출됨.
-- =============================================================================
