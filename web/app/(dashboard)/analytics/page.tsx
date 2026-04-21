"use client";

import { useEffect, useMemo, useState } from "react";
import { ArrowRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
  type ChipTone,
} from "@/components/editorial/primitives";

// ── 后端接口类型（不改动后端） ─────────────────────────────────

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
  const [trend, setTrend] = useState<DauPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [ov, tr] = await Promise.all([
          api.get<Overview>("/analytics/overview"),
          api.get<DauPoint[]>("/analytics/dau-trend"),
        ]);
        setOverview(ov);
        setTrend(tr);
      } catch (err) {
        console.error("加载分析数据失败:", err);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // 趋势摘要：用于 editorial lead 段落
  const summary = useMemo(() => {
    if (!trend.length) return null;
    const first = trend[0];
    const last = trend[trend.length - 1];
    const delta = last.dau - first.dau;
    return { first, last, delta };
  }, [trend]);

  return (
    <div>
      {/* ── Editorial lead ─────────────────────── */}
      <section className="mb-8 border-y border-line py-5">
        <Serif size={28}>
          近 14 天运营洞察 ·{" "}
          <span className="italic" style={{ color: "var(--accent)" }}>
            An editorial.
          </span>
        </Serif>
        <div className="mt-2.5 max-w-[720px] text-[13px] leading-relaxed text-muted">
          {loading ? (
            <Mono size={11}>— 加载中 —</Mono>
          ) : overview ? (
            <>
              今日 DAU{" "}
              <span
                className="font-serif text-[15px] text-ink"
                style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
              >
                {overview.dau}
              </span>
              ，月活{" "}
              <span
                className="font-serif text-[15px] text-ink"
                style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
              >
                {overview.mau}
              </span>
              ，今日触发事件{" "}
              <span
                className="font-serif text-[15px] text-ink"
                style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
              >
                {overview.todayEvents.toLocaleString()}
              </span>{" "}
              次。
              {summary && (
                <>
                  {" "}
                  DAU 较两周前{" "}
                  <span
                    style={{
                      color: summary.delta >= 0 ? "var(--moss)" : "var(--accent)",
                    }}
                  >
                    {summary.delta >= 0 ? "+" : ""}
                    {summary.delta}
                  </span>
                  。
                </>
              )}
            </>
          ) : (
            <Mono size={11}>— 暂无数据 —</Mono>
          )}
        </div>

        {/* KPI 小条 */}
        <div className="mt-5 grid grid-cols-3 border-t border-line pt-4">
          {[
            { lbl: "今日活跃", n: overview?.dau ?? 0, meta: "DAU" },
            { lbl: "月活用户", n: overview?.mau ?? 0, meta: "MAU" },
            {
              lbl: "今日事件",
              n: overview?.todayEvents ?? 0,
              meta: "EVENTS",
              accent: true,
            },
          ].map((k, i) => (
            <div
              key={i}
              className="px-6"
              style={{
                borderLeft: i > 0 ? "1px solid var(--line)" : "none",
              }}
            >
              <Eyebrow>{k.lbl}</Eyebrow>
              <div className="mt-1">
                <Numeral
                  size={38}
                  color={k.accent ? "var(--accent)" : "var(--ink)"}
                >
                  {loading ? "—" : k.n.toLocaleString()}
                </Numeral>
              </div>
              <div className="mt-1">
                <Mono size={10}>{k.meta}</Mono>
              </div>
            </div>
          ))}
        </div>
      </section>

      <div className="grid gap-8" style={{ gridTemplateColumns: "1.3fr 1fr" }}>
        {/* ── 左：DAU 趋势直方图 ───────────────── */}
        <div>
          <Eyebrow>DAU 趋势 · 近 14 天</Eyebrow>
          <div className="mt-3.5 border border-line bg-ivory p-6">
            {loading ? (
              <div className="py-10 text-center">
                <Mono size={11}>— 加载中 —</Mono>
              </div>
            ) : trend.length === 0 ? (
              <div className="py-10 text-center">
                <Mono size={11}>— 暂无数据 —</Mono>
              </div>
            ) : (
              <DauBarChart points={trend} />
            )}
          </div>

          {/* 事件事件数折线（简版） */}
          <div className="mt-8">
            <Eyebrow>事件趋势 · EVENTS</Eyebrow>
            <div className="mt-3.5 border border-line bg-ivory p-6">
              {loading ? (
                <div className="py-10 text-center">
                  <Mono size={11}>— 加载中 —</Mono>
                </div>
              ) : trend.length === 0 ? (
                <div className="py-10 text-center">
                  <Mono size={11}>— 暂无数据 —</Mono>
                </div>
              ) : (
                <EventTrendLine points={trend} />
              )}
            </div>
          </div>
        </div>

        {/* ── 右：热门事件 + 导出 ───────────────── */}
        <div>
          <Eyebrow>热门事件 · TOP EVENTS</Eyebrow>
          <div className="mt-3.5">
            {loading ? (
              <div className="border border-line bg-ivory py-10 text-center">
                <Mono size={11}>— 加载中 —</Mono>
              </div>
            ) : !overview?.topEvents?.length ? (
              <div className="border border-line bg-ivory py-10 text-center">
                <Mono size={11}>— 暂无事件数据 —</Mono>
              </div>
            ) : (
              <div>
                {overview.topEvents.slice(0, 6).map((e, i) => {
                  const tones: ChipTone[] = ["accent", "moss", "warn", "muted", "muted", "muted"];
                  return (
                    <div
                      key={i}
                      className="flex gap-3.5 border-b border-line py-4"
                    >
                      <Serif
                        size={18}
                        italic
                        color="var(--muted-2)"
                        style={{ width: 28 }}
                      >
                        {String(i + 1).padStart(2, "0")}
                      </Serif>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-baseline justify-between gap-3">
                          <Serif size={15}>
                            {e.eventName.replace(/_/g, " ")}
                          </Serif>
                          <Chip tone={tones[i]}>
                            {i === 0 ? "领先" : i < 3 ? "活跃" : "常规"}
                          </Chip>
                        </div>
                        <div className="mt-1 flex items-baseline gap-2">
                          <Numeral size={20}>
                            {Number(e.count).toLocaleString()}
                          </Numeral>
                          <Mono size={10}>近 30 天</Mono>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* 导出 hero */}
          <div
            className="relative mt-8 overflow-hidden px-6 py-5"
            style={{ background: "var(--ink)", color: "var(--ivory)" }}
          >
            <div
              className="pointer-events-none absolute inset-0 opacity-40"
              style={{
                backgroundImage:
                  "radial-gradient(rgba(251,248,242,0.12) 1px, transparent 1px)",
                backgroundSize: "14px 14px",
              }}
            />
            <div className="relative">
              <Eyebrow color="rgba(251,248,242,0.55)">导出与分享</Eyebrow>
              <div className="mt-2.5">
                <Serif size={19} color="var(--ivory)">
                  生成运营周报
                </Serif>
                <div>
                  <Serif size={15} italic color="var(--accent)">
                    In one click.
                  </Serif>
                </div>
              </div>
              <div className="mt-3.5 flex gap-2">
                <button
                  className="px-3.5 py-2 text-[12px]"
                  style={{
                    border: "1px solid rgba(251,248,242,0.25)",
                    background: "transparent",
                    color: "var(--ivory)",
                    borderRadius: 2,
                  }}
                >
                  PDF
                </button>
                <HairlineBtn
                  style={{ background: "var(--ivory)", color: "var(--ink)", border: 0 }}
                  rightIcon={
                    <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
                  }
                >
                  生成周报
                </HairlineBtn>
              </div>
              <div className="mt-3">
                <Mono size={9} color="rgba(251,248,242,0.45)">
                  * 本期仅预留入口，待后端导出接口上线后启用
                </Mono>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── DAU 直方图（editorial SVG） ──────────────────────────────────

function DauBarChart({ points }: { points: DauPoint[] }) {
  const max = Math.max(...points.map((p) => p.dau), 1);
  return (
    <div>
      <div
        className="flex items-end gap-1.5"
        style={{ height: 180, borderBottom: "1px solid var(--line)" }}
      >
        {points.map((p, i) => {
          const h = (p.dau / max) * 160;
          const peak = p.dau === max;
          return (
            <div
              key={i}
              className="flex h-full flex-1 flex-col items-center justify-end"
            >
              <span
                className="mb-1 font-serif"
                style={{
                  fontSize: 11,
                  color: peak ? "var(--accent)" : "var(--muted)",
                  fontVariationSettings: '"opsz" 144, "SOFT" 50',
                }}
              >
                {p.dau}
              </span>
              <div
                style={{
                  width: "100%",
                  height: h,
                  background: peak ? "var(--accent)" : "var(--ink)",
                  opacity: peak ? 1 : 0.85,
                }}
              />
            </div>
          );
        })}
      </div>
      <div className="mt-1.5 flex gap-1.5">
        {points.map((p, i) => (
          <span
            key={i}
            className="flex-1 text-center font-mono"
            style={{ fontSize: 10, color: "var(--muted)" }}
          >
            {p.date.slice(5)}
          </span>
        ))}
      </div>
      <div className="mt-3.5 flex justify-between">
        <Mono size={10}>PEAK · {Math.max(...points.map((p) => p.dau))}</Mono>
        <Mono size={10}>
          AVG ·{" "}
          {Math.round(
            points.reduce((s, p) => s + p.dau, 0) / points.length
          ).toLocaleString()}
        </Mono>
        <Mono size={10}>N = {points.length}</Mono>
      </div>
    </div>
  );
}

// ── 事件趋势折线（editorial SVG） ─────────────────────────────────

function EventTrendLine({ points }: { points: DauPoint[] }) {
  const W = 560;
  const H = 140;
  const pad = { t: 16, r: 10, b: 24, l: 36 };
  const max = Math.max(...points.map((p) => p.events), 1);
  const min = 0;

  const x = (i: number) =>
    pad.l + (i * (W - pad.l - pad.r)) / Math.max(points.length - 1, 1);
  const y = (v: number) =>
    pad.t + ((max - v) * (H - pad.t - pad.b)) / Math.max(max - min, 1);
  const path = points
    .map((p, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(p.events)}`)
    .join(" ");

  const yTicks = [min, Math.round(max * 0.5), max];

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
            {v.toLocaleString()}
          </text>
        </g>
      ))}
      {points.map((p, i) => (
        <text
          key={i}
          x={x(i)}
          y={H - 6}
          fill="var(--muted)"
          fontSize="9"
          fontFamily="var(--font-mono)"
          textAnchor="middle"
        >
          {p.date.slice(5)}
        </text>
      ))}
      <path d={path} fill="none" stroke="var(--accent)" strokeWidth="1.6" />
      {points.map((p, i) => (
        <circle
          key={i}
          cx={x(i)}
          cy={y(p.events)}
          r={2.5}
          fill="var(--accent)"
        />
      ))}
    </svg>
  );
}
