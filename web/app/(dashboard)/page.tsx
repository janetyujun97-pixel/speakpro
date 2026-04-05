"use client";

import { useEffect, useState } from "react";
import { Users, BookOpen, BarChart3, CheckCircle } from "lucide-react";
import { api } from "@/lib/api";
import { StatsCard } from "@/components/dashboard/stats-card";
import { ScoreTrendChart } from "@/components/dashboard/score-trend-chart";
import { StudentRankTable } from "@/components/dashboard/student-rank-table";

interface DashboardStats {
  activeStudents: number;
  totalSessions: number;
  averageScore: number;
  completionRate: string;
}

interface TrendDataPoint {
  date: string;
  pronunciation: number;
  fluency: number;
  grammar: number;
  overall: number;
}

interface LeaderboardEntry {
  rank: number;
  name: string;
  email: string;
  avgScore: number;
  totalSessions: number;
}

const DEFAULT_STATS: DashboardStats = {
  activeStudents: 0,
  totalSessions: 0,
  averageScore: 0,
  completionRate: "-",
};

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats>(DEFAULT_STATS);
  const [trendData, setTrendData] = useState<TrendDataPoint[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const fetchAll = async () => {
      const result = { ...DEFAULT_STATS };
      let firstClassId: string | null = null;

      // 1) KPI 统计
      try {
        const practiceStats = await api.get<{
          totalSessions: number;
          averageScore: number;
        }>("/practice/stats");
        result.totalSessions = practiceStats.totalSessions || 0;
        result.averageScore = practiceStats.averageScore || 0;
      } catch {}

      try {
        const classes = await api.get<
          { id: string; students: { id: string }[] | null }[]
        >("/classes");
        const studentIds = new Set<string>();
        for (const cls of classes) {
          if (cls.students) {
            for (const s of cls.students) studentIds.add(s.id);
          }
        }
        result.activeStudents = studentIds.size;
        // 记住第一个班级 ID 用于图表数据
        if (classes.length > 0) firstClassId = classes[0].id;
      } catch {}

      try {
        const assignments = await api.get<
          { submissions: { status: string }[] | null }[]
        >("/assignments");
        let totalSubs = 0;
        let gradedSubs = 0;
        for (const a of assignments) {
          if (a.submissions) {
            totalSubs += a.submissions.length;
            gradedSubs += a.submissions.filter(
              (s) => s.status === "graded"
            ).length;
          }
        }
        result.completionRate =
          totalSubs > 0
            ? `${Math.round((gradedSubs / totalSubs) * 100)}%`
            : "-";
      } catch {}

      setStats(result);

      // 2) 图表数据：使用第一个班级
      if (firstClassId) {
        try {
          const trends = await api.get<TrendDataPoint[]>(
            `/classes/${firstClassId}/score-trends`
          );
          setTrendData(trends);
        } catch {}

        try {
          const board = await api.get<LeaderboardEntry[]>(
            `/classes/${firstClassId}/leaderboard`
          );
          setLeaderboard(board);
        } catch {}
      }

      setLoaded(true);
    };

    fetchAll();
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">数据看板</h1>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="活跃学生"
          value={loaded ? stats.activeStudents : "-"}
          trend="neutral"
          icon={Users}
        />
        <StatsCard
          title="练习总数"
          value={loaded ? stats.totalSessions : "-"}
          trend="neutral"
          icon={BookOpen}
        />
        <StatsCard
          title="平均分"
          value={
            loaded
              ? stats.averageScore > 0
                ? stats.averageScore.toFixed(1)
                : "-"
              : "-"
          }
          trend="neutral"
          icon={BarChart3}
        />
        <StatsCard
          title="作业完成率"
          value={loaded ? stats.completionRate : "-"}
          trend="neutral"
          icon={CheckCircle}
        />
      </div>

      {/* Charts & Tables */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ScoreTrendChart data={trendData} />
        <StudentRankTable students={leaderboard} />
      </div>
    </div>
  );
}
