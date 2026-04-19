"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { AudioPlayer } from "@/components/grading/audio-player";
import { TranscriptViewer, type TranscriptSegment } from "@/components/grading/transcript-viewer";
import { ScoreAdjuster, type ScoreCategory } from "@/components/grading/score-adjuster";
import { VoiceMemoRecorder } from "@/components/grading/voice-memo-recorder";

// ---- 类型定义 ----

interface Assignment {
  id: string;
  title: string;
  questionIds: string[];
  submissions: Submission[];
}

interface Submission {
  id: string;
  studentId: string;
  student?: { name: string; email: string };
  sessionIds: string[];
  status: "pending" | "submitted" | "graded";
  teacherScore: number | null;
  teacherComment: string | null;
  teacherVoiceUrl: string | null;
  submittedAt: string | null;
  gradedAt: string | null;
}

interface PracticeSession {
  id: string;
  mode: string;
  audioUrl?: string;
  transcript?: string;
  overallScore?: number;
  pronunciationScore?: { overall?: number; score?: number };
  fluencyScore?: { overall?: number; score?: number };
  grammarScore?: { overall?: number; score?: number };
  contentScore?: { overall?: number; score?: number };
  aiFeedback?: string;
  question?: { promptText?: string; examType?: string; section?: string };
  createdAt: string;
}

// ---- 工具函数 ----

/** 从 transcript 文本生成 TranscriptSegment 数组 */
function parseTranscript(transcript?: string): TranscriptSegment[] {
  if (!transcript) return [];
  // 简单处理：整段作为学生发言
  const words = transcript.split(/\s+/).filter(Boolean).map((text) => ({ text }));
  if (words.length === 0) return [];
  return [{ speaker: "student" as const, timestamp: "0:00", words }];
}

/** 从 session 提取 AI 评分 categories */
function buildScoreCategories(session: PracticeSession): ScoreCategory[] {
  const getScore = (obj?: { overall?: number; score?: number }) =>
    obj?.overall ?? obj?.score ?? 0;

  return [
    { label: "发音准确度", key: "pronunciation", aiScore: getScore(session.pronunciationScore), maxScore: 100 },
    { label: "流利度", key: "fluency", aiScore: getScore(session.fluencyScore), maxScore: 100 },
    { label: "语法正确性", key: "grammar", aiScore: getScore(session.grammarScore), maxScore: 100 },
    { label: "内容相关性", key: "content", aiScore: getScore(session.contentScore), maxScore: 100 },
  ];
}

// ---- 主组件 ----

export default function GradePage() {
  const params = useParams();
  const assignmentId = params.id as string;

  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [selectedSubmission, setSelectedSubmission] = useState<Submission | null>(null);
  const [sessions, setSessions] = useState<PracticeSession[]>([]);
  const [selectedSession, setSelectedSession] = useState<PracticeSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [grading, setGrading] = useState(false);

  // 教师评分
  const [teacherScore, setTeacherScore] = useState(0);
  const [comment, setComment] = useState("");
  const [teacherVoiceUrl, setTeacherVoiceUrl] = useState<string | null>(null);

  // 加载作业
  useEffect(() => {
    loadAssignment();
  }, [assignmentId]);

  async function loadAssignment() {
    try {
      const data = await api.get<Assignment>(`/assignments/${assignmentId}`);
      setAssignment(data);
      const firstPending = data.submissions?.find((s) => s.status === "submitted");
      if (firstPending) selectSubmission(firstPending);
    } catch (err) {
      console.error("加载作业失败:", err);
    } finally {
      setLoading(false);
    }
  }

  // 选中学生提交
  async function selectSubmission(sub: Submission) {
    setSelectedSubmission(sub);
    setTeacherScore(sub.teacherScore ?? 70);
    setComment(sub.teacherComment ?? "");
    setTeacherVoiceUrl(sub.teacherVoiceUrl ?? null);
    setSelectedSession(null);
    setSessions([]);

    // 批量加载练习 sessions
    if (sub.sessionIds?.length > 0) {
      setSessionsLoading(true);
      try {
        const data = await api.post<PracticeSession[]>("/practice/sessions/batch", {
          sessionIds: sub.sessionIds,
        });
        setSessions(data);
        if (data.length > 0) setSelectedSession(data[0]);
      } catch (err) {
        console.error("加载练习记录失败:", err);
      } finally {
        setSessionsLoading(false);
      }
    }
  }

  // 提交评分
  async function handleGrade() {
    if (!selectedSubmission) return;
    setGrading(true);
    try {
      await api.put(`/assignments/${assignmentId}/grade`, {
        submissionId: selectedSubmission.id,
        teacherScore,
        teacherComment: comment,
        teacherVoiceUrl,
      });
      await loadAssignment();
    } catch {
      alert("评分提交失败");
    } finally {
      setGrading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-pulse space-y-2 text-center">
          <div className="h-4 w-32 bg-gray-200 rounded mx-auto" />
          <p className="text-gray-400 text-sm">加载中...</p>
        </div>
      </div>
    );
  }

  if (!assignment) {
    return <div className="text-center py-16 text-gray-400">作业不存在</div>;
  }

  const submissions = assignment.submissions || [];
  const submittedSubs = submissions.filter((s) => s.status !== "pending");
  const gradedCount = submissions.filter((s) => s.status === "graded").length;

  return (
    <div className="flex gap-6 h-[calc(100vh-120px)]">
      {/* 左侧：提交列表 */}
      <div className="w-72 shrink-0 bg-white rounded-xl shadow-sm overflow-y-auto">
        <div className="p-4 border-b">
          <h2 className="font-bold text-lg">{assignment.title}</h2>
          <p className="text-sm text-gray-500 mt-1">
            提交 {submittedSubs.length} 份 · 已批 {gradedCount} 份
          </p>
        </div>
        <div className="divide-y">
          {submittedSubs.length === 0 ? (
            <p className="p-4 text-center text-gray-400 text-sm">暂无学生提交</p>
          ) : (
            submittedSubs.map((sub) => (
              <button
                key={sub.id}
                onClick={() => selectSubmission(sub)}
                className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
                  selectedSubmission?.id === sub.id
                    ? "bg-blue-50 border-l-4 border-blue-600"
                    : ""
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-sm">
                    {sub.student?.name || sub.studentId.slice(0, 8)}
                  </span>
                  <span
                    className={`text-xs px-2 py-0.5 rounded-full ${
                      sub.status === "graded"
                        ? "bg-green-100 text-green-700"
                        : "bg-yellow-100 text-yellow-700"
                    }`}
                  >
                    {sub.status === "graded" ? `${sub.teacherScore}分` : "待批"}
                  </span>
                </div>
                <p className="text-xs text-gray-400 mt-1">
                  {sub.sessionIds?.length || 0} 个练习 ·{" "}
                  {sub.submittedAt
                    ? new Date(sub.submittedAt).toLocaleDateString()
                    : ""}
                </p>
              </button>
            ))
          )}
        </div>
      </div>

      {/* 右侧：批改详情 */}
      <div className="flex-1 overflow-y-auto space-y-4">
        {selectedSubmission ? (
          <>
            {/* 学生信息 + 会话选择 */}
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-lg font-bold">
                  {selectedSubmission.student?.name || "学生"} 的提交
                </h3>
                <span className="text-sm text-gray-400">
                  {sessions.length} 个练习会话
                </span>
              </div>

              {/* 会话选择 tabs */}
              {sessions.length > 1 && (
                <div className="flex gap-2 overflow-x-auto pb-1">
                  {sessions.map((s, i) => (
                    <button
                      key={s.id}
                      onClick={() => setSelectedSession(s)}
                      className={`px-3 py-1.5 text-xs rounded-lg whitespace-nowrap transition-colors ${
                        selectedSession?.id === s.id
                          ? "bg-blue-600 text-white"
                          : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                      }`}
                    >
                      练习 #{i + 1}
                      {s.overallScore != null && ` (${s.overallScore}分)`}
                    </button>
                  ))}
                </div>
              )}

              {sessionsLoading && (
                <p className="text-sm text-gray-400 animate-pulse mt-2">加载练习数据...</p>
              )}
            </div>

            {/* 选中的会话详情 */}
            {selectedSession && (
              <>
                {/* 题目信息 */}
                {selectedSession.question && (
                  <div className="bg-white rounded-xl shadow-sm p-4">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                        {selectedSession.question.examType}
                      </span>
                      {selectedSession.question.section && (
                        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                          {selectedSession.question.section}
                        </span>
                      )}
                      <span className="text-xs text-gray-400">
                        {selectedSession.mode}
                      </span>
                    </div>
                    <p className="text-sm text-gray-700">
                      {selectedSession.question.promptText}
                    </p>
                  </div>
                )}

                {/* 音频播放器 */}
                <AudioPlayer src={selectedSession.audioUrl} />

                {/* 转写文本 */}
                <TranscriptViewer
                  segments={parseTranscript(selectedSession.transcript)}
                />

                {/* AI 反馈 */}
                {selectedSession.aiFeedback && (
                  <div className="bg-white rounded-xl border border-border p-4 space-y-2">
                    <h3 className="font-semibold text-primary">AI 反馈</h3>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
                      {selectedSession.aiFeedback}
                    </p>
                  </div>
                )}

                {/* 维度评分调节 */}
                <ScoreAdjuster
                  categories={buildScoreCategories(selectedSession)}
                  onScoresChange={(_scores, total) => setTeacherScore(total)}
                />
              </>
            )}

            {/* 教师评语 + 提交 */}
            <div className="bg-white rounded-xl shadow-sm p-4 space-y-4">
              <h4 className="font-medium text-gray-700">教师评语</h4>

              <div className="flex items-center gap-4">
                <span className="text-sm text-gray-500">综合评分</span>
                <span className="text-3xl font-bold text-blue-600">{teacherScore}</span>
                <span className="text-sm text-gray-400">/ 100</span>
              </div>

              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={3}
                placeholder="给学生写一些鼓励或改进建议..."
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 text-sm"
              />

              <VoiceMemoRecorder
                existingUrl={teacherVoiceUrl}
                onUploaded={(url) => setTeacherVoiceUrl(url)}
                onCleared={() => setTeacherVoiceUrl(null)}
              />

              <button
                onClick={handleGrade}
                disabled={grading}
                className="w-full py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium transition-colors"
              >
                {grading
                  ? "提交中..."
                  : selectedSubmission.status === "graded"
                  ? "更新评分"
                  : "提交评分"}
              </button>
            </div>
          </>
        ) : (
          <div className="flex items-center justify-center h-full bg-white rounded-xl shadow-sm text-gray-400">
            <p>请从左侧选择一个学生提交进行批改</p>
          </div>
        )}
      </div>
    </div>
  );
}
