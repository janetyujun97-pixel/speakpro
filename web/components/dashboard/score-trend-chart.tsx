"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const sampleData = [
  { date: "3/25", 发音: 78, 流利度: 72, 语法: 68 },
  { date: "3/26", 发音: 80, 流利度: 74, 语法: 70 },
  { date: "3/27", 发音: 79, 流利度: 76, 语法: 73 },
  { date: "3/28", 发音: 82, 流利度: 78, 语法: 71 },
  { date: "3/29", 发音: 85, 流利度: 80, 语法: 75 },
  { date: "3/30", 发音: 84, 流利度: 82, 语法: 78 },
  { date: "3/31", 发音: 87, 流利度: 83, 语法: 80 },
];

export function ScoreTrendChart() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>成绩趋势</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={sampleData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis
                dataKey="date"
                fontSize={12}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                fontSize={12}
                tickLine={false}
                axisLine={false}
                domain={[0, 100]}
              />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="发音"
                stroke="#3B82F6"
                strokeWidth={2}
                dot={{ r: 4 }}
              />
              <Line
                type="monotone"
                dataKey="流利度"
                stroke="#10B981"
                strokeWidth={2}
                dot={{ r: 4 }}
              />
              <Line
                type="monotone"
                dataKey="语法"
                stroke="#F59E0B"
                strokeWidth={2}
                dot={{ r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
