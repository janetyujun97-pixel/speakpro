"use client";

import { useEffect, useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, BarChart, Bar, Legend,
} from "recharts";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface Overview {
  dau: number;
  mau: number;
  todayEvents: number;
  topEvents: { eventName: string; count: string }[];
}

interface DauPoint {
  date: string;
  dau: number;
  events: number;
}

export default function AnalyticsPage() {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [dauTrend, setDauTrend] = useState<DauPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [ov, trend] = await Promise.all([
          api.get<Overview>("/analytics/overview"),
          api.get<DauPoint[]>("/analytics/dau-trend"),
        ]);
        setOverview(ov);
        setDauTrend(trend);
      } catch (err) {
        console.error("加载分析数据失败:", err);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return <div className="flex justify-center py-16 text-gray-400">加载中...</div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">运营分析</h1>

      {/* KPI 卡片 */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">今日活跃用户 (DAU)</p>
            <p className="text-3xl font-bold text-primary">{overview?.dau ?? 0}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">月活跃用户 (MAU)</p>
            <p className="text-3xl font-bold text-accent">{overview?.mau ?? 0}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground">今日事件数</p>
            <p className="text-3xl font-bold text-data-blue">{overview?.todayEvents ?? 0}</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* DAU 趋势 */}
        <Card>
          <CardHeader>
            <CardTitle>DAU 趋势（近 14 天）</CardTitle>
          </CardHeader>
          <CardContent>
            {dauTrend.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">暂无数据</p>
            ) : (
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={dauTrend.map(d => ({ ...d, date: d.date.slice(5) }))}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="date" fontSize={12} tickLine={false} />
                    <YAxis fontSize={12} tickLine={false} />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="dau" name="DAU" stroke="#3B82F6" strokeWidth={2} dot={{ r: 3 }} />
                    <Line type="monotone" dataKey="events" name="事件数" stroke="#10B981" strokeWidth={2} dot={{ r: 3 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 热门事件 */}
        <Card>
          <CardHeader>
            <CardTitle>热门事件（近 30 天）</CardTitle>
          </CardHeader>
          <CardContent>
            {!overview?.topEvents?.length ? (
              <p className="text-center text-muted-foreground py-10 text-sm">暂无数据</p>
            ) : (
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={overview.topEvents.map(e => ({
                    name: e.eventName.replace(/_/g, ' '),
                    count: parseInt(e.count),
                  }))}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="name" fontSize={10} tickLine={false} angle={-30} textAnchor="end" height={60} />
                    <YAxis fontSize={12} tickLine={false} />
                    <Tooltip />
                    <Bar dataKey="count" name="次数" fill="#FF6B4A" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
