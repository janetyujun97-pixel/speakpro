"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";

// ── 数据类型（与后端现有接口对齐）────────────────────────────────────

interface DashboardStats {
  activeStudents: number;
  totalSessions: number;
  averageScore: number;   // 后端以 0–100 返回；展示时换算成 0–9.0
  completionRate: string; // "87%" / "-"
  toGrade: number;
}

interface TrendPoint {
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

// ── 常量 ─────────────────────────────────────────────────────────────

const DEFAULT_STATS: DashboardStats = {
  activeStudents: 0,
  totalSessions: 0,
  averageScore: 0,
  completionRate: "-",
  toGrade: 0,
};

// 六维雷达占位数据（后端尚无字段，待 `/dashboard/ability-profile` 上线替换）
const AXIS_PLACEHOLDER = [
  { k: "发音", v: 0, en: "Pron." },
  { k: "流利", v: 0, en: "Fluency" },
  { k: "语法", v: 0, en: "Gram." },
  { k: "词汇", v: 0, en: "Lex." },
  { k: "连贯", v: 0, en: "Coh." },
  { k: "互动", v: 0, en: "Inter." },
];

// ── 主页面 ───────────────────────────────────────────────────────────

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats>(DEFAULT_STATS);
  const [trends, setTrends] = useState<TrendPoint[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [firstClassName, setFirstClassName] = useState<string>("");
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const run = async () => {
      const next = { ...DEFAULT_STATS };
      let firstClassId: string | null = null;

      // KPI 1: 练习总数 + 平均分
      try {
        const ps = await api.get<{ totalSessions: number; averageScore: number }>(
          "/practice/stats"
        );
        next.totalSessions = ps.totalSessions || 0;
        next.averageScore = ps.averageScore || 0;
      } catch {}

      // KPI 2: 活跃学生
      try {
        const classes = await api.get<
          { id: string; name?: string; students: { id: string }[] | null }[]
        >("/classes");
        const ids = new Set<string>();
        for (const c of classes) for (const s of c.students || []) ids.add(s.id);
        next.activeStudents = ids.size;
        if (classes[0]) {
          firstClassId = classes[0].id;
          setFirstClassName(classes[0].name || "");
        }
      } catch {}

      // KPI 3: 作业完成率 + 待批改数
      try {
        const assigns = await api.get<
          { submissions: { status: string }[] | null }[]
        >("/assignments");
        let total = 0;
        let graded = 0;
        let pending = 0;
        for (const a of assigns) {
          for (const s of a.submissions || []) {
            total++;
            if (s.status === "graded") graded++;
            else if (s.status === "submitted") pending++;
          }
        }
        next.completionRate = total > 0 ? `${Math.round((graded / total) * 100)}%` : "-";
        next.toGrade = pending;
      } catch {}

      setStats(next);

      // 图表 + 排行榜
      if (firstClassId) {
        try {
          const t = await api.get<TrendPoint[]>(`/classes/${firstClassId}/score-trends`);
          setTrends(t);
        } catch {}
        try {
          const b = await api.get<LeaderboardEntry[]>(
            `/classes/${firstClassId}/leaderboard`
          );
          setLeaderboard(b);
        } catch {}
      }

      setLoaded(true);
    };
    run();
  }, []);

  // 后端平均分范围 0–100；展示换算 0–9.0（雅思风格）
  const avg9 = loaded && stats.averageScore > 0
    ? ((stats.averageScore / 100) * 9).toFixed(1)
    : "-";

  return (
    <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-[1.4fr_1fr]">
      {/* ── LEFT COLUMN ───────────────────────────────────────── */}
      <div>
        {/* KPI 条 */}
        <KpiStrip
          items={[
            { lbl: "活跃学生", n: loaded ? String(stats.activeStudents) : "-", unit: "人", meta: "统计全部班级" },
            { lbl: "练习总数", n: loaded ? stats.totalSessions.toLocaleString() : "-", unit: "", meta: "累计" },
            { lbl: "平均分",  n: avg9,                                                   unit: "", meta: "满分 9.0", accent: true },
            { lbl: "完成率",  n: loaded ? stats.completionRate : "-",                    unit: "", meta: "作业按时提交" },
          ]}
        />

        {/* 专题：六维雷达（当前为占位，等后端字段）*/}
        <section className="mt-9">
          <div className="mb-4 flex items-baseline justify-between">
            <Eyebrow>专题 · FEATURED REPORT</Eyebrow>
            <Mono size={10}>FIG. 01 / 04</Mono>
          </div>

          <div
            className="relative overflow-hidden rounded-xs px-[30px] py-7"
            style={{ background: "var(--ink)", color: "var(--ivory)" }}
          >
            <div
              className="pointer-events-none absolute inset-0 opacity-50"
              style={{
                backgroundImage: "radial-gradient(rgba(251,248,242,0.12) 1px, transparent 1px)",
                backgroundSize: "14px 14px",
              }}
            />
            <div className="relative">
              <div className="flex items-start justify-between">
                <div>
                  <Eyebrow color="rgba(251,248,242,0.55)">六维能力诊断 · 本周</Eyebrow>
                  <div className="mt-3">
                    <Serif size={30} color="var(--ivory)">全班平均能力轮廓</Serif>
                    <div className="mt-1">
                      <Serif size={22} italic color="var(--accent)">Six-axis profile.</Serif>
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <Mono size={10} color="rgba(251,248,242,0.55)">
                    SAMPLE N = {loaded ? stats.activeStudents : "—"}
                  </Mono>
                  <div className="mt-1.5">
                    <Numeral size={34} color="var(--ivory)">{avg9}</Numeral>
                    <span className="ml-1.5 text-xs" style={{ color: "rgba(251,248,242,0.55)" }}>
                      / 9.0
                    </span>
                  </div>
                </div>
              </div>

              {/* 六维条形 — 占位 */}
              <div className="mt-6 grid grid-cols-6 gap-4">
                {AXIS_PLACEHOLDER.map((a, i) => (
                  <AxisBar key={i} axis={a} highlight={i === 5} />
                ))}
              </div>

              <div
                className="mt-5 flex items-center justify-between border-t pt-4"
                style={{ borderColor: "rgba(251,248,242,0.15)" }}
              >
                <Mono size={10} color="rgba(251,248,242,0.55)">
                  占位数据 · 等后端六维评分字段上线后自动填充
                </Mono>
                <HairlineBtn
                  style={{ background: "var(--ivory)", color: "var(--ink)", border: 0 }}
                  rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
                >
                  下发建议练习
                </HairlineBtn>
              </div>
            </div>
          </div>
        </section>

        {/* 四周均分走势 */}
        <section className="mt-10">
          <div className="mb-3 flex items-baseline justify-between">
            <Eyebrow>四周均分走势 · TREND</Eyebrow>
            <div className="flex gap-3.5">
              <LegendSwatch color="var(--ink)" label="总分" />
              <LegendSwatch color="var(--accent)" label="语法" dashed />
            </div>
          </div>
          <div className="rounded-xs border border-line bg-ivory px-6 py-5">
            <TrendChart points={trends} />
          </div>
        </section>
      </div>

      {/* ── RIGHT COLUMN ──────────────────────────────────────── */}
      <div>
        {/* 待批改票根 */}
        <ToGradeTicket count={stats.toGrade} />

        {/* 排行榜 */}
        <section className="mt-9">
          <div className="mb-3.5 flex items-baseline justify-between">
            <Eyebrow>学生排名 · LEADERBOARD</Eyebrow>
            {firstClassName && <Mono size={10}>{firstClassName}</Mono>}
          </div>

          <div className="border-t border-line">
            {leaderboard.length === 0 ? (
              <div className="border-b border-line py-10 text-center">
                <Mono size={10}>— 暂无学生数据 —</Mono>
              </div>
            ) : (
              leaderboard.slice(0, 5).map((r) => (
                <div
                  key={r.rank}
                  className="flex items-center gap-3.5 border-b border-line py-3.5"
                >
                  <Serif
                    size={22}
                    italic
                    color={r.rank <= 3 ? "var(--accent)" : "var(--muted-2)"}
                    style={{ width: 34 }}
                  >
                    {String(r.rank).padStart(2, "0")}
                  </Serif>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-ink">{r.name}</div>
                    <Mono size={10}>
                      {r.email} · {r.totalSessions} sessions
                    </Mono>
                  </div>
                  <div className="text-right">
                    <Numeral size={22}>{r.avgScore.toFixed(1)}</Numeral>
                  </div>
                </div>
              ))
            )}
          </div>

          {leaderboard.length > 5 && (
            <div className="mt-3 text-center">
              <Link href="/classes">
                <Mono size={10}>— 查看全部学生 —</Mono>
              </Link>
            </div>
          )}
        </section>

        {/* 今日动态 — 暂用占位（无后端实时流） */}
        <section className="mt-9">
          <Eyebrow>今日动态 · LIVE</Eyebrow>
          <div className="mt-3.5">
            <div className="border-b border-line-soft py-6 text-center">
              <Mono size={10}>— 实时动态流待接入 —</Mono>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

// ── 子组件 ───────────────────────────────────────────────────────────

interface KpiItem {
  lbl: string;
  n: string;
  unit: string;
  meta: string;
  accent?: boolean;
}

function KpiStrip({ items }: { items: KpiItem[] }) {
  return (
    <div className="grid grid-cols-4 border-y border-line py-6">
      {items.map((k, i) => (
        <div
          key={i}
          className="px-6"
          style={{ borderLeft: i > 0 ? "1px solid var(--line)" : "none" }}
        >
          <Eyebrow>{k.lbl}</Eyebrow>
          <div className="mt-2 flex items-baseline">
            <Numeral size={48} color={k.accent ? "var(--accent)" : "var(--ink)"}>
              {k.n}
            </Numeral>
            {k.unit && <span className="ml-1 text-[13px] text-muted">{k.unit}</span>}
          </div>
          <div className="mt-1.5">
            <Mono size={10}>{k.meta}</Mono>
          </div>
        </div>
      ))}
    </div>
  );
}

function AxisBar({
  axis,
  highlight,
}: {
  axis: { k: string; v: number; en: string };
  highlight: boolean;
}) {
  const pct = Math.max(0, Math.min(100, (axis.v / 9) * 100));
  return (
    <div>
      <div
        className="flex h-[90px] items-end"
        style={{ borderBottom: "1px solid rgba(251,248,242,0.15)" }}
      >
        <div
          className="w-full"
          style={{
            height: `${pct}%`,
            background: highlight ? "var(--accent)" : "rgba(251,248,242,0.7)",
            borderTop: "2px solid var(--ivory)",
          }}
        />
      </div>
      <div className="mt-2.5">
        <div className="text-[11px] font-semibold" style={{ color: "var(--ivory)" }}>
          {axis.k}
        </div>
        <div className="mt-px">
          <Serif size={11} italic color="rgba(251,248,242,0.5)">
            {axis.en}
          </Serif>
        </div>
        <div className="mt-1">
          <Serif size={18} color={highlight ? "var(--accent)" : "var(--ivory)"}>
            {axis.v > 0 ? axis.v.toFixed(1) : "—"}
          </Serif>
        </div>
      </div>
    </div>
  );
}

function LegendSwatch({ color, label, dashed }: { color: string; label: string; dashed?: boolean }) {
  return (
    <div className="flex items-center gap-1.5">
      <span
        className="inline-block h-0.5 w-3"
        style={{
          background: dashed ? "transparent" : color,
          borderTop: dashed ? `2px dashed ${color}` : "none",
        }}
      />
      <Mono size={10}>{label}</Mono>
    </div>
  );
}

function ToGradeTicket({ count }: { count: number }) {
  return (
    <section>
      <div
        className="relative flex overflow-hidden rounded-xs border border-line"
        style={{ background: "var(--ivory)" }}
      >
        <div
          className="flex w-28 shrink-0 flex-col justify-between px-4 py-5"
          style={{ background: "var(--accent)", color: "var(--ivory)" }}
        >
          <Eyebrow color="rgba(251,248,242,0.75)">TO GRADE</Eyebrow>
          <div>
            <Numeral size={54} color="var(--ivory)">{count}</Numeral>
            <div className="mt-0.5 text-[11px] opacity-85">pending</div>
          </div>
        </div>
        {/* 票根穿孔装饰 */}
        <div
          className="absolute -top-[7px] h-3.5 w-3.5 rounded-full border border-line"
          style={{ left: 112, background: "var(--bg)" }}
        />
        <div
          className="absolute -bottom-[7px] h-3.5 w-3.5 rounded-full border border-line"
          style={{ left: 112, background: "var(--bg)" }}
        />
        <div
          className="absolute bottom-2.5 top-2.5"
          style={{ left: 125, borderLeft: "1.5px dashed var(--line)" }}
        />

        <div className="flex-1 pb-4 pl-7 pr-5 pt-4">
          <Eyebrow>待批改作业 · QUEUE</Eyebrow>
          <div className="mt-2">
            <Serif size={19}>
              {count > 0 ? (
                <>
                  {count} 份待批改
                  <br />
                  提交
                </>
              ) : (
                <>
                  暂无待批改
                  <br />
                  作业
                </>
              )}
            </Serif>
          </div>
          <div className="mt-3 text-[12px] leading-relaxed text-muted">
            AI 已完成预评分
            <br />
            等你复核
          </div>
          <div className="mt-3.5 flex justify-end">
            <Link
              href="/assignments"
              className="inline-flex items-center gap-1.5 text-[12px] font-semibold"
              style={{ color: "var(--accent)" }}
            >
              进入批改 <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

/** 走势图：接收 TrendPoint[]，为空时画空图 */
function TrendChart({ points }: { points: TrendPoint[] }) {
  const W = 560;
  const H = 180;
  const pad = { t: 20, r: 10, b: 28, l: 32 };
  const yMin = 5.0;
  const yMax = 7.0;
  const safe = points.length > 0 ? points : [];

  const x = (i: number, total: number) =>
    pad.l + (i * (W - pad.l - pad.r)) / Math.max(total - 1, 1);
  const y = (v: number) =>
    pad.t + ((yMax - v) * (H - pad.t - pad.b)) / (yMax - yMin);
  const path = (arr: number[]) =>
    arr.map((v, i) => `${i === 0 ? "M" : "L"}${x(i, arr.length)},${y(v)}`).join(" ");

  // 后端返回的分数是 0–100；换算到 0–9
  const to9 = (v: number) => (v / 100) * 9;

  const totalLine = safe.map((p) => to9(p.overall));
  const gramLine = safe.map((p) => to9(p.grammar));

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="block h-auto w-full">
      {/* Y 网格 */}
      {[5.0, 5.5, 6.0, 6.5, 7.0].map((v) => (
        <g key={v}>
          <line
            x1={pad.l}
            x2={W - pad.r}
            y1={y(v)}
            y2={y(v)}
            stroke="var(--line)"
            strokeDasharray={v === 6.0 ? "0" : "2 3"}
            strokeWidth={v === 6.0 ? 1 : 0.6}
          />
          <text
            x={pad.l - 8}
            y={y(v) + 4}
            fill="var(--muted)"
            fontSize="10"
            fontFamily="var(--font-mono)"
            textAnchor="end"
          >
            {v.toFixed(1)}
          </text>
        </g>
      ))}
      {/* X 标签 */}
      {safe.length > 0 &&
        safe.map((p, i) => (
          <text
            key={i}
            x={x(i, safe.length)}
            y={H - 10}
            fill="var(--muted)"
            fontSize="10"
            fontFamily="var(--font-mono)"
            textAnchor="middle"
          >
            {p.date.slice(5)}
          </text>
        ))}
      {/* 线 */}
      {totalLine.length > 1 && (
        <path d={path(totalLine)} fill="none" stroke="var(--ink)" strokeWidth="1.6" />
      )}
      {gramLine.length > 1 && (
        <path
          d={path(gramLine)}
          fill="none"
          stroke="var(--accent)"
          strokeWidth="1.6"
          strokeDasharray="4 2"
        />
      )}
      {/* 点 */}
      {totalLine.map((v, i) => (
        <circle
          key={"t" + i}
          cx={x(i, totalLine.length)}
          cy={y(v)}
          r={3}
          fill="var(--ivory)"
          stroke="var(--ink)"
          strokeWidth={1.4}
        />
      ))}
      {gramLine.map((v, i) => (
        <circle
          key={"g" + i}
          cx={x(i, gramLine.length)}
          cy={y(v)}
          r={2.5}
          fill="var(--accent)"
        />
      ))}
      {/* 空状态 */}
      {safe.length === 0 && (
        <text
          x={W / 2}
          y={H / 2}
          fill="var(--muted)"
          fontSize="11"
          fontFamily="var(--font-mono)"
          textAnchor="middle"
        >
          — 暂无趋势数据 —
        </text>
      )}
    </svg>
  );
}
