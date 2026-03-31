import { Users, BookOpen, BarChart3, CheckCircle } from "lucide-react";
import { StatsCard } from "@/components/dashboard/stats-card";
import { ScoreTrendChart } from "@/components/dashboard/score-trend-chart";
import { StudentRankTable } from "@/components/dashboard/student-rank-table";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">数据看板</h1>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="活跃学生"
          value={128}
          trend="up"
          trendValue="较上周 +12%"
          icon={Users}
        />
        <StatsCard
          title="本周练习"
          value={356}
          trend="up"
          trendValue="较上周 +8%"
          icon={BookOpen}
        />
        <StatsCard
          title="平均分"
          value={82.5}
          trend="up"
          trendValue="较上周 +3.2"
          icon={BarChart3}
        />
        <StatsCard
          title="作业完成率"
          value="94%"
          trend="neutral"
          trendValue="与上周持平"
          icon={CheckCircle}
        />
      </div>

      {/* Charts & Tables */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ScoreTrendChart />
        <StudentRankTable />
      </div>
    </div>
  );
}
