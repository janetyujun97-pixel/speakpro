"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";

interface StudentProfile {
  id: string;
  name: string;
  email: string;
}

interface SessionItem {
  id: string;
  mode: string;
  overallScore: number | null;
  createdAt: string;
}

interface StatsData {
  totalSessions: number;
  averageScore: number;
  totalDurationMin: number;
  dimensions: { pronunciation: number; fluency: number; grammar: number; content: number } | null;
}

export default function StudentDetailPage() {
  const params = useParams();
  const classId = params.id as string;
  const studentId = params.sid as string;

  const [sessions, setSessions] = useState<SessionItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStudentData();
  }, [studentId]);

  async function loadStudentData() {
    try {
      // 加载该学生的练习记录
      const data = await api.get<SessionItem[]>(
        `/practice/sessions?student_id=${studentId}`
      );
      setSessions(Array.isArray(data) ? data : []);
    } catch {
      console.error("加载学生数据失败");
    } finally {
      setLoading(false);
    }
  }

  const avgScore =
    sessions.filter((s) => s.overallScore != null).length > 0
      ? sessions
          .filter((s) => s.overallScore != null)
          .reduce((sum, s) => sum + (s.overallScore || 0), 0) /
        sessions.filter((s) => s.overallScore != null).length
      : 0;

  const modeCount: Record<string, number> = {};
  sessions.forEach((s) => {
    modeCount[s.mode] = (modeCount[s.mode] || 0) + 1;
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">学生详情</h1>

      {loading ? (
        <p className="text-gray-500">加载中...</p>
      ) : (
        <>
          {/* 统计卡片 */}
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-white p-5 rounded-xl shadow-sm">
              <p className="text-sm text-gray-500">练习总数</p>
              <p className="text-3xl font-bold text-blue-600 mt-1">{sessions.length}</p>
            </div>
            <div className="bg-white p-5 rounded-xl shadow-sm">
              <p className="text-sm text-gray-500">平均分</p>
              <p className="text-3xl font-bold text-green-600 mt-1">{avgScore.toFixed(1)}</p>
            </div>
            <div className="bg-white p-5 rounded-xl shadow-sm">
              <p className="text-sm text-gray-500">练习模式分布</p>
              <div className="flex gap-2 mt-2 flex-wrap">
                {Object.entries(modeCount).map(([mode, count]) => (
                  <span key={mode} className="text-xs px-2 py-1 bg-gray-100 rounded">
                    {mode}: {count}
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* 练习记录列表 */}
          <div className="bg-white rounded-xl shadow-sm">
            <div className="p-4 border-b">
              <h2 className="font-semibold">练习记录</h2>
            </div>
            {sessions.length === 0 ? (
              <p className="p-8 text-center text-gray-400">暂无练习记录</p>
            ) : (
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="text-left px-4 py-3 text-sm font-medium text-gray-500">日期</th>
                    <th className="text-left px-4 py-3 text-sm font-medium text-gray-500">模式</th>
                    <th className="text-left px-4 py-3 text-sm font-medium text-gray-500">得分</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {sessions.slice(0, 20).map((s) => (
                    <tr key={s.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm">{new Date(s.createdAt).toLocaleString()}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className="px-2 py-0.5 bg-blue-50 text-blue-700 text-xs rounded">{s.mode}</span>
                      </td>
                      <td className="px-4 py-3 text-sm font-medium">
                        {s.overallScore != null ? `${s.overallScore.toFixed(1)}` : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
