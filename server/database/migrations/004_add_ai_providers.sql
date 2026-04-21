-- 为用户补充 ASR / ISE / LLM 提供商偏好
-- 对应: 002_add_tts_provider.sql 已存在的 tts_provider
-- ASR: tencent / xunfei
-- ISE: tencent / xunfei
-- LLM: mimo / qwen

BEGIN;

ALTER TABLE users ADD COLUMN IF NOT EXISTS asr_provider VARCHAR(20) DEFAULT 'tencent';
ALTER TABLE users ADD COLUMN IF NOT EXISTS ise_provider VARCHAR(20) DEFAULT 'tencent';
ALTER TABLE users ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(20) DEFAULT 'mimo';

COMMIT;
