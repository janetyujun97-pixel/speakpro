"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";

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
  submittedAt: string | null;
  gradedAt: string | null;
}

export default function GradePage() {
  const params = useParams();
  const assignmentId = params.id as string;

  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [selectedSubmission, setSelectedSubmission] = useState<Submission | null>(null);
  const [loading, setLoading] = useState(true);
  const [grading, setGrading] = useState(false);

  // 评分表单
  const [score, setScore] = useState(0);
  const [comment, setComment] = useState("");

  useEffect(() => {
    loadAssignment();
  }, [assignmentId]);

  async function loadAssignment() {
    try {
      const data = await api.get<Assignment>(`/assignments/${assignmentId}`);
      setAssignment(data);
      // 自动选中第一个待批改的提交
      const firstPending = data.submissions?.find((s) => s.status === "submitted");
      if (firstPending) selectSubmission(firstPending);
    } catch (err) {
      console.error("加载作业失败:", err);
    } finally {
      setLoading(false);
    }
  }

  function selectSubmission(sub: Submission) {
    setSelectedSubmission(sub);
    setScore(sub.teacherScore ?? 70);
    setComment(sub.teacherComment ?? "");
  }

  async function handleGrade() {
    if (!selectedSubmission) return;
    setGrading(true);
    try {
      await api.put(`/assignments/${assignmentId}/grade`, {
        submissionId: selectedSubmission.id,
        teacherScore: score,
        teacherComment: comment,
      });
      // 刷新数据
      await loadAssignment();
      setGrading(false);
    } catch {
      alert("评分提交失败");
      setGrading(false);
    }
  }

  if (loading) {
    return <div className="flex items-center justify-center h-64"><p className="text-gray-500">加载中...</p></div>;
  }

  if (!assignment) {
    return <div className="text-center py-16 text-gray-400">作业不存在</div>;
  }

  const submissions = assignment.submissions || [];
  const submittedCount = submissions.filter((s) => s.status !== "pending").length;
  const gradedCount = submissions.filter((s) => s.status === "graded").length;

  return (
    <div className="flex gap-6 h-[calc(100vh-120px)]">
      {/* 左侧：提交列表 */}
      <div className="w-80 shrink-0 bg-white rounded-xl shadow-sm overflow-y-auto">
        <div className="p-4 border-b">
          <h2 className="font-bold text-lg">{assignment.title}</h2>
          <p className="text-sm text-gray-500 mt-1">
            提交 {submittedCount} 份 · 已批 {gradedCount} 份
          </p>
        </div>
        <div className="divide-y">
          {submissions.length === 0 ? (
            <p className="p-4 text-center text-gray-400 text-sm">暂无学生提交</p>
          ) : (
            submissions
              .filter((s) => s.status !== "pending")
              .map((sub) => (
                <button
                  key={sub.id}
                  onClick={() => selectSubmission(sub)}
                  className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
                    selectedSubmission?.id === sub.id ? "bg-blue-50 border-l-4 border-blue-600" : ""
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
                    {sub.submittedAt
                      ? new Date(sub.submittedAt).toLocaleString()
                      : "未提交"}
                  </p>
                </button>
              ))
          )}
        </div>
      </div>

      {/* 右侧：批改详情 */}
      <div className="flex-1 bg-white rounded-xl shadow-sm overflow-y-auto">
        {selectedSubmission ? (
          <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold">
                {selectedSubmission.student?.name || "学生"} 的提交
              </h3>
              <span className="text-sm text-gray-400">
                练习 {selectedSubmission.sessionIds?.length || 0} 个会话
              </span>
            </div>

            {/* 练习会话列表 */}
            <div className="space-y-3">
              <h4 className="font-medium text-gray-700">练习记录</h4>
              {selectedSubmission.sessionIds?.length > 0 ? (
                selectedSubmission.sessionIds.map((sid, i) => (
                  <div key={sid} className="border rounded-lg p-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium">练习 #{i + 1}</span>
                      <span className="text-xs text-gray-400">ID: {sid.slice(0, 8)}...</span>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      可查看该练习的音频、转写和 AI 评分
                    </p>
                  </div>
                ))
              ) : (
                <p className="text-gray-400 text-sm">无练习记录</p>
              )}
            </div>

            {/* 教师评分区 */}
            <div className="border-t pt-6 space-y-4">
              <h4 className="font-medium text-gray-700">教师评分</h4>

              <div>
                <label className="block text-sm text-gray-600 mb-1">
                  总分 (0-100)
                </label>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={score}
                  onChange={(e) => setScore(Number(e.target.value))}
                  className="w-full"
                />
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400">0</span>
                  <span className="text-2xl font-bold text-blue-600">{score}</span>
                  <span className="text-gray-400">100</span>
                </div>
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">
                  评语
                </label>
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  rows={3}
                  placeholder="给学生写一些鼓励或改进建议..."
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <button
                onClick={handleGrade}
                disabled={grading}
                className="w-full py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium"
              >
                {grading
                  ? "提交中..."
                  : selectedSubmission.status === "graded"
                  ? "更新评分"
                  : "提交评分"}
              </button>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            <p>请从左侧选择一个学生提交进行批改</p>
          </div>
        )}
      </div>
    </div>
  );
}
