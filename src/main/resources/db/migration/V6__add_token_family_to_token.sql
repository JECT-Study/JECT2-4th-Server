-- Add token_family column for refresh token rotation + reuse detection
-- tokenFamily groups a chain of rotated refresh tokens belonging to the same login session/device.
-- When a revoked token from the same family is presented during reissue, the entire family is revoked (reuse attack detection).

ALTER TABLE token
    ADD COLUMN token_family VARCHAR(255);

CREATE INDEX idx_token_family ON token (token_family);