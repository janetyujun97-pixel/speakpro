-- SpeakPro 种子数据
-- 用于开发和测试环境
-- 执行方式: psql -U speakpro -d speakpro -f seed.sql

BEGIN;

-- ==================== 用户 ====================
-- 教师账号: teacher@speakpro.com / teacher123
INSERT INTO users (id, role, email, password, name) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'teacher', 'teacher@speakpro.com',
   '$2b$10$PTqqlaf1IQ8oPeE/AWeGGuTy9iAaIpbVaJiGuEQ/c/meVILarzyTS', '王老师'),
  ('a0000000-0000-0000-0000-000000000002', 'admin', 'admin@speakpro.com',
   '$2b$10$PTqqlaf1IQ8oPeE/AWeGGuTy9iAaIpbVaJiGuEQ/c/meVILarzyTS', '管理员')
ON CONFLICT (email) DO NOTHING;

-- 学生账号: student1@speakpro.com / student123
INSERT INTO users (id, role, email, password, name) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'student', 'student1@speakpro.com',
   '$2b$10$UkBXAa7m/3w3bebAP3Idpu5fRZEN1YeMYpymcrukq8Rqo4.L18zJC', '张三'),
  ('b0000000-0000-0000-0000-000000000002', 'student', 'student2@speakpro.com',
   '$2b$10$UkBXAa7m/3w3bebAP3Idpu5fRZEN1YeMYpymcrukq8Rqo4.L18zJC', '李四'),
  ('b0000000-0000-0000-0000-000000000003', 'student', 'student3@speakpro.com',
   '$2b$10$UkBXAa7m/3w3bebAP3Idpu5fRZEN1YeMYpymcrukq8Rqo4.L18zJC', '王五')
ON CONFLICT (email) DO NOTHING;

-- ==================== 班级 ====================
INSERT INTO classes (id, name, teacher_id, exam_type) VALUES
  ('c0000000-0000-0000-0000-000000000001', '雅思冲刺A班', 'a0000000-0000-0000-0000-000000000001', 'IELTS'),
  ('c0000000-0000-0000-0000-000000000002', '托福基础B班', 'a0000000-0000-0000-0000-000000000001', 'TOEFL')
ON CONFLICT DO NOTHING;

-- ==================== 学生-班级关联 ====================
INSERT INTO class_students (class_id, student_id) VALUES
  ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001'),
  ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002'),
  ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- ==================== 题库 ====================
-- IELTS 题目
INSERT INTO questions (id, exam_type, section, topic, prompt_text, difficulty, tags, created_by) VALUES
  ('d0000000-0000-0000-0000-000000000001', 'IELTS', 'Part1', 'Hometown',
   'Where do you come from? What do you like about your hometown?',
   2, '["hometown", "daily life"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000002', 'IELTS', 'Part1', 'Study',
   'What subject are you studying? Why did you choose this subject?',
   2, '["study", "education"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000003', 'IELTS', 'Part2', 'A Person',
   'Describe a person who has influenced you. You should say: who this person is, how you know this person, what this person does, and explain why this person has influenced you.',
   3, '["people", "influence"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000004', 'IELTS', 'Part2', 'An Event',
   'Describe a memorable event in your life. You should say: what the event was, when it happened, who was involved, and explain why it was memorable.',
   3, '["event", "memory"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000005', 'IELTS', 'Part3', 'Education',
   'How has education changed in your country in recent years? Do you think online education will replace traditional education?',
   4, '["education", "technology"]', 'a0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- TOEFL 题目
INSERT INTO questions (id, exam_type, section, topic, prompt_text, difficulty, tags, created_by) VALUES
  ('d0000000-0000-0000-0000-000000000006', 'TOEFL', 'Independent', 'Preference',
   'Some people prefer to study alone, while others prefer to study in groups. Which do you prefer and why?',
   3, '["preference", "study"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000007', 'TOEFL', 'Independent', 'Opinion',
   'Do you agree or disagree with the following statement: Technology has made our lives easier. Use specific reasons and examples to support your answer.',
   3, '["technology", "opinion"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000008', 'TOEFL', 'Integrated', 'Campus',
   'The university plans to close the campus library on weekends. The student in the conversation disagrees with this plan. Summarize the reasons.',
   4, '["campus", "integrated"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000009', 'TOEFL', 'Independent', 'Daily Life',
   'What is the most important quality of a good neighbor? Use specific details and examples in your response.',
   2, '["daily life", "community"]', 'a0000000-0000-0000-0000-000000000001'),
  ('d0000000-0000-0000-0000-000000000010', 'TOEFL', 'Independent', 'Education',
   'Some people believe that university students should be required to attend classes. Others believe that going to classes should be optional. Which view do you support?',
   3, '["education", "university"]', 'a0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- ==================== 作业 ====================
INSERT INTO assignments (id, title, description, class_id, teacher_id, question_ids, due_date) VALUES
  ('e0000000-0000-0000-0000-000000000001', 'IELTS Speaking 专项训练',
   '完成以下5道雅思口语练习题，注意发音和流利度。',
   'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   ARRAY['d0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002',
         'd0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004',
         'd0000000-0000-0000-0000-000000000005']::UUID[],
   NOW() + INTERVAL '7 days')
ON CONFLICT DO NOTHING;

COMMIT;
