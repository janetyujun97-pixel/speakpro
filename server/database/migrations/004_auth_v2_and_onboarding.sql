-- PR2a · Auth v2 + Onboarding
-- 新增：手机 OTP / Apple / 微信三方登录字段；onboarding profile；OTP 失败审计；邮箱 reset token

BEGIN;

-- ========== users 表扩展 ==========

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS apple_sub      VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS wechat_unionid VARCHAR(64)  UNIQUE;

-- 手机/Apple/微信注册时无密码，放开 NOT NULL
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_phone          ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_apple_sub      ON users(apple_sub);
CREATE INDEX IF NOT EXISTS idx_users_wechat_unionid ON users(wechat_unionid);

-- ========== onboarding_profiles ==========

CREATE TABLE IF NOT EXISTS onboarding_profiles (
    user_id              UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    exam_type            VARCHAR(20) CHECK (exam_type IN ('IELTS','TOEFL','GENERAL')),
    target_score         NUMERIC(3,1),
    exam_date            DATE,
    self_level           SMALLINT CHECK (self_level BETWEEN 1 AND 5),
    baseline_session_id  UUID REFERENCES practice_sessions(id) ON DELETE SET NULL,
    study_plan           JSONB,
    completed_at         TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_onboarding_completed ON onboarding_profiles(completed_at);

-- ========== auth_otp_attempts ==========
-- Redis 做热路径计数，DB 做持久审计

CREATE TABLE IF NOT EXISTS auth_otp_attempts (
    phone         VARCHAR(20) PRIMARY KEY,
    fail_count    SMALLINT    NOT NULL DEFAULT 0,
    locked_until  TIMESTAMPTZ,
    last_sent_at  TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ========== email_reset_tokens ==========
-- 教师端邮件重置密码（手机端走 OTP + /auth/reset-password，不用这张表）

CREATE TABLE IF NOT EXISTS email_reset_tokens (
    token        VARCHAR(128) PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_reset_user    ON email_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_reset_expires ON email_reset_tokens(expires_at);

COMMIT;
