-- token.expires_at: TIMESTAMP -> TIMESTAMP WITH TIME ZONE
-- LocalDateTime -> Instant 매핑 변경에 따른 컬럼 타입 정렬 (기존 값은 UTC로 간주)
ALTER TABLE token
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE
    USING expires_at AT TIME ZONE 'UTC';
