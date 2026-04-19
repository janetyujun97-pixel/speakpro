-- PR3a · 老师语音备注
-- Web 教师后台批改页会录一段语音 → 上传到 OSS → 把 URL 落到这张表
-- 学生端练习 / 作业详情展示此语音条目

BEGIN;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS teacher_voice_url TEXT;

-- submissions 也支持老师对单个提交录一段语音备注（批改回复用）
ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS teacher_voice_url TEXT;

COMMIT;
