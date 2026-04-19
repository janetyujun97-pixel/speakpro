-- PR3a · 通知中心
-- - notifications：作业 / 批改 / 打卡 / 系统通知列表
-- - user_notification_prefs：免打扰时段 + push 开关（v1 仅作字段落库，后续 PR 接 APNs / FCM）

BEGIN;

CREATE TABLE IF NOT EXISTS notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind        VARCHAR(20) NOT NULL
                    CHECK (kind IN ('homework','feedback','streak','reminder','system')),
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    payload     JSONB,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications(user_id) WHERE is_read = FALSE;

CREATE TABLE IF NOT EXISTS user_notification_prefs (
    user_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    quiet_start   TIME        NOT NULL DEFAULT '22:30',
    quiet_end     TIME        NOT NULL DEFAULT '07:30',
    push_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

COMMIT;
