"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
} from "@/components/editorial/primitives";

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
      } catch {
        setSessions([]);
      } finally {
        setLoading(false);
      }
    })();
  }, [studentId]);

  const scored = useMemo(
    () => sessions.filter((s) => s.overallScore != null),
    [sessions]
  );

  const dimAvg = (field: keyof Session) => {
    const vals = scored
      .map(
        (s) =>
          (s[field] as { overall?: number; score?: number })?.overall ??
          (s[field] as { overall?: number; score?: number })?.score
      )
      .filter((v): v is number => v != null);
    return vals.length > 0
      ? Math.round(vals.reduce((a, b) => a + b, 0) / vals.length)
      : 0;
  };

  const radar = [
    { k: "发音", en: "Pron.",  v: dimAvg("pronunciationScore") },
    { k: "流利", en: "Flu.",   v: dimAvg("fluencyScore") },
    { k: "语法", en: "Gram.",  v: dimAvg("grammarScore") },
    { k: "内容", en: "Con.",   v: dimAvg("contentScore") },
  ];

  const trend = scored
    .slice(-20)
    .map((s) => ({
      date: new Date(s.createdAt).toLocaleDateString("zh-CN", {
        month: "numeric",
        day: "numeric",
      }),
      score: Number(s.overallScore) || 0,
    }));

  const avgScore =
    scored.length > 0
      ? Math.round(scored.reduce((s, e) => s + Number(e.overallScore), 0) / scored.length)
      : 0;

  const modeMap: Record<string, number> = {};
  for (const s of sessions) modeMap[s.mode] = (modeMap[s.mode] || 0) + 1;

  if (loading) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 加载中 —</Mono>
      </div>
    );
  }

  return (
    <div>
      {/* Masthead */}
      <div className="mb-8 border-b border-line pb-5">
        <Eyebrow>学生档案 · STUDENT</Eyebrow>
        <div className="mt-2">
          <Serif size={30}>学员学习画像</Serif>
        </div>
        <div className="mt-1.5">
          <Mono size={10}>ID · {studentId.slice(0, 8).toUpperCase()}</Mono>
        </div>
      </div>

      {/* KPI 条 */}
      <div className="mb-10 grid grid-cols-3 border-y border-line py-6">
        <KpiCell label="总练习次数" value={sessions.length.toLocaleString()} meta="累计所有模式" />
        <KpiCell
          label="平均得分"
          value={avgScore > 0 ? avgScore.toString() : "—"}
          meta={`基于 ${scored.length} 次评分`}
          accent
          left
        />
        <div className="px-6" style={{ borderLeft: "1px solid var(--line)" }}>
          <Eyebrow>练习模式</Eyebrow>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {Object.entries(modeMap).length === 0 ? (
              <Mono size={10}>— 暂无 —</Mono>
            ) : (
              Object.entries(modeMap).map(([m, c]) => (
                <Chip key={m} tone="muted">
                  {m} · {c}
                </Chip>
              ))
            )}
          </div>
        </div>
      </div>

      {/* 图表区 2-col */}
      <div className="grid gap-8" style={{ gridTemplateColumns: "1fr 1fr" }}>
        {/* 四维能力条 */}
        <div>
          <Eyebrow>维度分析 · DIMENSIONS</Eyebrow>
          <div className="mt-3.5 border border-line bg-ivory p-6">
            {scored.length === 0 ? (
              <div className="py-10 text-center">
                <Mono size={11}>— 暂无评分数据 —</Mono>
              </div>
            ) : (
              <div className="space-y-4">
                {radar.map((d) => (
                  <div key={d.k}>
                    <div className="flex items-baseline justify-between">
                      <div className="flex items-baseline gap-2">
                        <span className="text-[13px] text-ink">{d.k}</span>
                        <Serif size={11} italic color="var(--muted-2)">
                          {d.en}
                        </Serif>
                      </div>
                      <Numeral size={18}>{d.v}</Numeral>
                    </div>
                    <div
                      className="relative mt-1.5"
                      style={{ height: 3, background: "var(--line-soft)" }}
                    >
                      <div
                        className="absolute inset-y-0 left-0"
                        style={{
                          width: `${Math.max(0, Math.min(100, d.v))}%`,
                          background: "var(--ink)",
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 成长曲线 */}
        <div>
          <Eyebrow>成长曲线 · GROWTH</Eyebrow>
          <div className="mt-3.5 border border-line bg-ivory p-6">
            {trend.length === 0 ? (
              <div className="py-10 text-center">
                <Mono size={11}>— 暂无趋势数据 —</Mono>
              </div>
            ) : (
              <GrowthLine points={trend} />
            )}
          </div>
        </div>
      </div>

      {/* 练习记录表 */}
      <div className="mt-10">
        <Eyebrow>练习记录 · LATEST 20</Eyebrow>
        <div className="mt-3.5 border border-line bg-ivory">
          {sessions.length === 0 ? (
            <div className="py-10 text-center">
              <Mono size={11}>— 暂无练习记录 —</Mono>
            </div>
          ) : (
            <div>
              <div
                className="grid items-center gap-2 px-5 py-3"
                style={{
                  gridTemplateColumns: "40px 1.5fr 1fr 0.6fr",
                  background: "var(--bg-soft)",
                  borderBottom: "1px solid var(--line)",
                }}
              >
                {["№", "日期", "模式", "得分"].map((h) => (
                  <Eyebrow key={h} style={{ fontSize: 9 }}>
                    {h}
                  </Eyebrow>
                ))}
              </div>
              {sessions.slice(0, 20).map((s, i, arr) => (
                <div
                  key={s.id}
                  className="grid items-center gap-2 px-5 py-3"
                  style={{
                    gridTemplateColumns: "40px 1.5fr 1fr 0.6fr",
                    borderBottom: i < arr.length - 1 ? "1px solid var(--line-soft)" : 0,
                  }}
                >
                  <Serif size={15} italic color="var(--muted-2)">
                    {String(i + 1).padStart(2, "0")}
                  </Serif>
                  <Mono size={11} color="var(--ink)">
                    {new Date(s.createdAt).toLocaleString("zh-CN", {
                      year: "2-digit",
                      month: "2-digit",
                      day: "2-digit",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </Mono>
                  <Chip tone="muted">{s.mode}</Chip>
                  <div className="text-right">
                    <Numeral size={18}>
                      {s.overallScore != null ? s.overallScore : "—"}
                    </Numeral>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── helpers ──────────────────────────────────────────────────────────

function KpiCell({
  label,
  value,
  meta,
  accent,
  left,
}: {
  label: string;
  value: string;
  meta: string;
  accent?: boolean;
  left?: boolean;
}) {
  return (
    <div
      className="px-6"
      style={{ borderLeft: left ? "1px solid var(--line)" : "none" }}
    >
      <Eyebrow>{label}</Eyebrow>
      <div className="mt-1.5">
        <Numeral size={44} color={accent ? "var(--accent)" : "var(--ink)"}>
          {value}
        </Numeral>
      </div>
      <div className="mt-1">
        <Mono size={10}>{meta}</Mono>
      </div>
    </div>
  );
}

function GrowthLine({ points }: { points: { date: string; score: number }[] }) {
  const W = 420;
  const H = 180;
  const pad = { t: 16, r: 10, b: 28, l: 36 };
  const scores = points.map((p) => p.score);
  const max = Math.max(...scores, 100);
  const min = Math.min(0, ...scores);

  const x = (i: number) =>
    pad.l + (i * (W - pad.l - pad.r)) / Math.max(points.length - 1, 1);
  const y = (v: number) =>
    pad.t + ((max - v) * (H - pad.t - pad.b)) / Math.max(max - min, 1);
  const path = points
    .map((p, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(p.score)}`)
    .join(" ");

  const yTicks = [0, 50, 100];

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="block h-auto w-full">
      {yTicks.map((v, i) => (
        <g key={i}>
          <line
            x1={pad.l}
            x2={W - pad.r}
            y1={y(v)}
            y2={y(v)}
            stroke="var(--line)"
            strokeDasharray={i === 0 ? "0" : "2 3"}
            strokeWidth={i === 0 ? 1 : 0.6}
          />
          <text
            x={pad.l - 8}
            y={y(v) + 3}
            fill="var(--muted)"
            fontSize="10"
            fontFamily="var(--font-mono)"
            textAnchor="end"
          >
            {v}
          </text>
        </g>
      ))}
      {points.map((p, i) => (
        <text
          key={i}
          x={x(i)}
          y={H - 8}
          fill="var(--muted)"
          fontSize="9"
          fontFamily="var(--font-mono)"
          textAnchor="middle"
        >
          {p.date}
        </text>
      ))}
      <path d={path} fill="none" stroke="var(--accent)" strokeWidth="1.6" />
      {points.map((p, i) => (
        <circle
          key={i}
          cx={x(i)}
          cy={y(p.score)}
          r={2.5}
          fill="var(--ivory)"
          stroke="var(--accent)"
          strokeWidth={1.4}
        />
      ))}
    </svg>
  );
}
