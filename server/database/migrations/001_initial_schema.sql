-- SpeakPro 初始数据库 Schema
-- 执行方式: psql -U speakpro -d speakpro -f 001_initial_schema.sql

BEGIN;

-- 启用 UUID 扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role        VARCHAR(20) NOT NULL CHECK (role IN ('student', 'teacher', 'admin')),
    phone       VARCHAR(20) UNIQUE,
    email       VARCHAR(255) UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    avatar_url  TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 班级表
CREATE TABLE IF NOT EXISTS classes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    teacher_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    exam_type   VARCHAR(20) CHECK (exam_type IN ('TOEFL', 'IELTS', 'BOTH')),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 学生-班级关联
CREATE TABLE IF NOT EXISTS class_students (
    class_id    UUID REFERENCES classes(id) ON DELETE CASCADE,
    student_id  UUID REFERENCES users(id) ON DELETE CASCADE,
    joined_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (class_id, student_id)
);

-- 题库
CREATE TABLE IF NOT EXISTS questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_type       VARCHAR(10) NOT NULL,
    section         VARCHAR(50) NOT NULL,
    topic           VARCHAR(200),
    prompt_text     TEXT NOT NULL,
    sample_audio_url TEXT,
    sample_text     TEXT,
    difficulty      INT CHECK (difficulty BETWEEN 1 AND 5),
    tags            JSONB DEFAULT '[]',
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 练习记录
CREATE TABLE IF NOT EXISTS practice_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID REFERENCES users(id) ON DELETE CASCADE,
    question_id         UUID REFERENCES questions(id) ON DELETE SET NULL,
    mode                VARCHAR(20) NOT NULL,
    audio_url           TEXT,
    transcript          TEXT,
    duration_sec        INT,
    pronunciation_score JSONB,
    fluency_score       JSONB,
    grammar_score       JSONB,
    content_score       JSONB,
    overall_score       DECIMAL(5,2),
    ai_feedback         TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- 作业表
CREATE TABLE IF NOT EXISTS assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    class_id    UUID REFERENCES classes(id) ON DELETE CASCADE,
    teacher_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    question_ids UUID[] NOT NULL,
    due_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 作业提交
CREATE TABLE IF NOT EXISTS submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID REFERENCES assignments(id) ON DELETE CASCADE,
    student_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    session_ids     UUID[],
    status          VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'submitted', 'graded')),
    teacher_comment TEXT,
    teacher_score   DECIMAL(5,2),
    submitted_at    TIMESTAMPTZ,
    graded_at       TIMESTAMPTZ
);

-- 教学资源
CREATE TABLE IF NOT EXISTS resources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('audio', 'document', 'video', 'wordlist')),
    file_url    TEXT NOT NULL,
    file_size   BIGINT,
    exam_type   VARCHAR(10),
    tags        JSONB DEFAULT '[]',
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_classes_teacher ON classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_questions_exam_type ON questions(exam_type);
CREATE INDEX IF NOT EXISTS idx_questions_section ON questions(section);
CREATE INDEX IF NOT EXISTS idx_practice_student ON practice_sessions(student_id);
CREATE INDEX IF NOT EXISTS idx_practice_created ON practice_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_assignments_class ON assignments(class_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student ON submissions(student_id);

COMMIT;
