-- 为用户添加 TTS 提供商偏好设置
-- 可选值: mimo / fish / xunfei

BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS tts_provider VARCHAR(20) DEFAULT 'mimo';

COMMIT;
