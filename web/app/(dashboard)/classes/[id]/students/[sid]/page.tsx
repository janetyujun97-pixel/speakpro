"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar,
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
} from "recharts";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface Session {
  id: string;
  mode: string;
  overallScore?: number;
  pronunciationScore?: { overall?: number; score?: number };
  fluencyScore?: { overall?: number; score?: number };
  grammarScore?: { overall?: number; score?: number };
  contentScore?: { overall?: number; score?: number };
  createdAt: string;
}

export default function StudentDetailPage() {
  const { sid: studentId } = useParams() as { id: string; sid: string };
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const data = await api.get<Session[]>(
          `/practice/sessions?student_id=${studentId}`
        );
        setSessions(Array.isArray(data) ? data : []);
      } catch {}
      setLoading(false);
    })();
  }, [studentId]);

  if (loading) {
    return <div className="flex justify-center py-16 text-gray-400">加载中...</div>;
  }

  const scoredSessions = sessions.filter((s) => s.overallScore != null);

  // 维度平均分（雷达图）
  const dimAvg = (field: keyof Session) => {
    const vals = scoredSessions
      .map((s) => (s[field] as any)?.overall ?? (s[field] as any)?.score)
      .filter((v: any) => v != null);
    return vals.length > 0
      ? Math.round(vals.reduce((a: number, b: number) => a + b, 0) / vals.length)
      : 0;
  };

  const radarData = [
    { subject: "发音", score: dimAvg("pronunciationScore"), fullMark: 100 },
    { subject: "流利度", score: dimAvg("fluencyScore"), fullMark: 100 },
    { subject: "语法", score: dimAvg("grammarScore"), fullMark: 100 },
    { subject: "内容", score: dimAvg("contentScore"), fullMark: 100 },
  ];

  // 成长曲线（最近 20 次）
  const trendData = scoredSessions
    .slice(-20)
    .map((s) => ({
      date: new Date(s.createdAt).toLocaleDateString("zh-CN", { month: "numeric", day: "numeric" }),
      score: s.overallScore,
    }));

  const avgScore = scoredSessions.length > 0
    ? Math.round(scoredSessions.reduce((s, e) => s + Number(e.overallScore), 0) / scoredSessions.length)
    : 0;

  // 模式分布
  const modeMap: Record<string, number> = {};
  sessions.forEach((s) => { modeMap[s.mode] = (modeMap[s.mode] || 0) + 1; });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">学生学习档案</h1>

      {/* KPI 卡片 */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">总练习次数</p>
            <p className="text-3xl font-bold text-primary">{sessions.length}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">平均得分</p>
            <p className="text-3xl font-bold text-accent">{avgScore || "-"}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">练习模式</p>
            <div className="flex flex-wrap justify-center gap-1 mt-2">
              {Object.entries(modeMap).map(([mode, count]) => (
                <span key={mode} className="text-xs bg-gray-100 px-2 py-0.5 rounded">
                  {mode}: {count}
                </span>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 图表区 */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* 雷达图 */}
        <Card>
          <CardHeader><CardTitle>维度分析</CardTitle></CardHeader>
          <CardContent>
            {scoredSessions.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">暂无评分数据</p>
            ) : (
              <div className="h-[280px]">
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart data={radarData}>
                    <PolarGrid />
                    <PolarAngleAxis dataKey="subject" fontSize={12} />
                    <PolarRadiusAxis domain={[0, 100]} tick={false} />
                    <Radar dataKey="score" stroke="#3B82F6" fill="#3B82F6" fillOpacity={0.3} strokeWidth={2} />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 成长曲线 */}
        <Card>
          <CardHeader><CardTitle>成长曲线</CardTitle></CardHeader>
          <CardContent>
            {trendData.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">暂无趋势数据</p>
            ) : (
              <div className="h-[280px]">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={trendData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="date" fontSize={12} tickLine={false} />
                    <YAxis domain={[0, 100]} fontSize={12} tickLine={false} />
                    <Tooltip />
                    <Line type="monotone" dataKey="score" name="总分" stroke="#10B981" strokeWidth={2} dot={{ r: 3 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 练习历史 */}
      <Card>
        <CardHeader><CardTitle>练习记录</CardTitle></CardHeader>
        <CardContent>
          {sessions.length === 0 ? (
            <p className="text-center text-muted-foreground py-6 text-sm">暂无练习记录</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="pb-3 font-medium text-muted-foreground">日期</th>
                    <th className="pb-3 font-medium text-muted-foreground">模式</th>
                    <th className="pb-3 text-right font-medium text-muted-foreground">得分</th>
                  </tr>
                </thead>
                <tbody>
                  {sessions.slice(0, 20).map((s) => (
                    <tr key={s.id} className="border-b last:border-0">
                      <td className="py-2.5 text-muted-foreground">
                        {new Date(s.createdAt).toLocaleString("zh-CN")}
                      </td>
                      <td className="py-2.5">
                        <span className="text-xs bg-gray-100 px-2 py-0.5 rounded">{s.mode}</span>
                      </td>
                      <td className="py-2.5 text-right font-semibold">
                        {s.overallScore != null ? s.overallScore : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
