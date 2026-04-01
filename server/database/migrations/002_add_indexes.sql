-- 002_add_indexes.sql
-- 性能优化索引

-- 练习会话：按学生 + 时间查询（Dashboard 趋势、学生详情）
CREATE INDEX IF NOT EXISTS idx_practice_sessions_student_created
ON practice_sessions (student_id, created_at DESC);

-- 练习会话：按总分筛选有效评分
CREATE INDEX IF NOT EXISTS idx_practice_sessions_overall_score
ON practice_sessions (overall_score)
WHERE overall_score IS NOT NULL;

-- 提交：按状态查询（待批/已批）
CREATE INDEX IF NOT EXISTS idx_submissions_status
ON submissions (status);

-- 提交：按作业 + 学生查找
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_student
ON submissions (assignment_id, student_id);

-- 作业：按班级 + 时间排序
CREATE INDEX IF NOT EXISTS idx_assignments_class_created
ON assignments (class_id, created_at DESC);

-- 题目：按考试类型 + 题型筛选
CREATE INDEX IF NOT EXISTS idx_questions_exam_section
ON questions (exam_type, section);
