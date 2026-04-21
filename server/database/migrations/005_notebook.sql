-- PR3a · 错题本 / 生词本
-- - notebook_words：ISE 单词分 < 60 的词（由 Go orchestrator 跑完评测后 upsert）
--                  + SM-2 间隔复习（ef / interval_days / next_review_at）
-- - notebook_phrases：老师批注 / 好句摘录

BEGIN;

CREATE TABLE IF NOT EXISTS notebook_words (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word              VARCHAR(64) NOT NULL,
    ipa               VARCHAR(64),
    note              TEXT,
    source_session_id UUID REFERENCES practice_sessions(id) ON DELETE SET NULL,
    miss_count        INT NOT NULL DEFAULT 1,
    last_seen_at      TIMESTAMPTZ DEFAULT NOW(),
    mastered_at       TIMESTAMPTZ,
    next_review_at    TIMESTAMPTZ DEFAULT NOW(),
    ef                NUMERIC(3,2) NOT NULL DEFAULT 2.5,    -- easiness factor
    interval_days     INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, word)
);

CREATE INDEX IF NOT EXISTS idx_notebook_words_user          ON notebook_words(user_id);
CREATE INDEX IF NOT EXISTS idx_notebook_words_next_review   ON notebook_words(user_id, next_review_at)
    WHERE mastered_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notebook_words_mastered      ON notebook_words(user_id, mastered_at);

CREATE TABLE IF NOT EXISTS notebook_phrases (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phrase       TEXT NOT NULL,
    note         VARCHAR(64),
    use_count    INT NOT NULL DEFAULT 1,
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notebook_phrases_user ON notebook_phrases(user_id);

COMMIT;
