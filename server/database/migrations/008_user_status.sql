-- 008_user_status.sql
-- 用户账号启用/禁用状态：支持管理员后台禁用账号，禁用后无法登录
-- 旧数据默认 active

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'disabled'));

CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(role, status);
