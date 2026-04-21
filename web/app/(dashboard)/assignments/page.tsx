"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus, Download } from "lucide-react";
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

// ── Types ────────────────────────────────────────────────────────────

interface Submission {
  id: string;
  status: "pending" | "submitted" | "graded";
}

interface Assignment {
  id: string;
  title: string;
  description: string;
  classId: string;
  questionIds: string[];
  dueDate: string;
  submissions: Submission[] | null;
  createdAt: string;
}

interface ClassOption {
  id: string;
  name: string;
  students?: { id: string }[] | null;
}

type StatusKey = "done" | "progress" | "draft";

// ── 常量 ─────────────────────────────────────────────────────────────

const TABS = [
  { key: "all",      label: "全部"     },
  { key: "progress", label: "进行中"   },
  { key: "done",     label: "已截止"   },
  { key: "draft",    label: "草稿"     },
] as const;

type TabKey = (typeof TABS)[number]["key"];

const PAGE_SIZE = 10;

const STATUS_META: Record<StatusKey, { label: string; tone: ChipTone }> = {
  done:     { label: "已截止", tone: "moss"    },
  progress: { label: "进行中", tone: "accent"  },
  draft:    { label: "草稿",   tone: "muted"   },
};

// ── 工具函数 ─────────────────────────────────────────────────────────

function deriveStatus(a: Assignment): StatusKey {
  if (!a.submissions || a.submissions.length === 0) return "draft";
  if (a.dueDate && new Date(a.dueDate) < new Date()) return "done";
  return "progress";
}

function submissionRate(subs: Submission[] | null) {
  const total = subs?.length || 0;
  const active = (subs || []).filter((s) => s.status !== "pending").length;
  return {
    total,
    active,
    pct: total > 0 ? Math.round((active / total) * 100) : 0,
  };
}

function formatDue(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${mm}·${dd} ${hh}:${mi}`;
}

// ── 页面 ─────────────────────────────────────────────────────────────

const GRID_COLS = "40px 2.2fr 1fr 1fr 1fr 1fr 0.8fr 80px";

export default function AssignmentsPage() {
  const router = useRouter();
  const [items, setItems] = useState<Assignment[]>([]);
  const [classMap, setClassMap] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState<TabKey>("all");
  const [page, setPage] = useState(1);

  useEffect(() => {
    (async () => {
      try {
        const [assigns, classes] = await Promise.all([
          api.get<Assignment[]>("/assignments"),
          api.get<ClassOption[]>("/classes").catch(() => [] as ClassOption[]),
        ]);
        setItems(assigns);
        setClassMap(Object.fromEntries(classes.map((c) => [c.id, c.name])));
      } catch (err) {
        setError(err instanceof Error ? err.message : "加载作业列表失败");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const filtered = useMemo(() => {
    if (tab === "all") return items;
    return items.filter((a) => deriveStatus(a) === tab);
  }, [items, tab]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pageItems = filtered.slice(
    (safePage - 1) * PAGE_SIZE,
    safePage * PAGE_SIZE
  );

  return (
    <div>
      {/* 操作条 */}
      <div className="mb-5 flex items-center gap-3">
        <Mono size={10}>筛选 · FILTER</Mono>
        {TABS.map((t) => {
          const on = tab === t.key;
          return (
            <button
              key={t.key}
              onClick={() => {
                setTab(t.key);
                setPage(1);
              }}
              className="px-3 py-1.5 text-[12px] transition-colors"
              style={{
                background: "transparent",
                color: on ? "var(--ink)" : "var(--muted)",
                borderBottom: `1px solid ${on ? "var(--ink)" : "transparent"}`,
              }}
            >
              {t.label}
            </button>
          );
        })}
        <div className="flex-1" />
        <HairlineBtn
          leftIcon={<Download className="h-[13px] w-[13px]" strokeWidth={1.3} />}
        >
          导出
        </HairlineBtn>
        <Link href="/assignments/new">
          <HairlineBtn
            primary
            leftIcon={<Plus className="h-[13px] w-[13px]" strokeWidth={1.5} />}
          >
            布置新作业
          </HairlineBtn>
        </Link>
      </div>

      {/* Error */}
      {error && (
        <div
          className="mb-6 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      {/* 表格容器 */}
      <div className="rounded-xs border border-line bg-ivory">
        {/* 表头 */}
        <div
          className="grid items-center gap-2 px-5.5 py-3.5"
          style={{
            gridTemplateColumns: GRID_COLS,
            padding: "14px 22px",
            background: "var(--bg-soft)",
            borderBottom: "1px solid var(--line)",
          }}
        >
          {["№", "题目", "类型", "班级", "截止", "提交率", "均分", ""].map(
            (h, i) => (
              <Eyebrow key={i} style={{ fontSize: 9 }}>
                {h}
              </Eyebrow>
            )
          )}
        </div>

        {/* 状态：加载 / 空 / 行 */}
        {loading ? (
          <div className="py-16 text-center">
            <Mono size={11}>— 加载中 —</Mono>
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center">
            <Mono size={11}>— 暂无作业 —</Mono>
          </div>
        ) : (
          pageItems.map((a, idx) => {
            const status = deriveStatus(a);
            const rate = submissionRate(a.submissions);
            const barColor =
              rate.pct === 100 ? "var(--moss)" : "var(--accent)";
            const rowIdx = (safePage - 1) * PAGE_SIZE + idx + 1;

            return (
              <button
                key={a.id}
                onClick={() => router.push(`/assignments/${a.id}/grade`)}
                className="group grid w-full items-center gap-2 text-left transition-colors hover:bg-bg-soft/50"
                style={{
                  gridTemplateColumns: GRID_COLS,
                  padding: "18px 22px",
                  borderBottom:
                    idx < pageItems.length - 1 ? "1px solid var(--line)" : 0,
                }}
              >
                {/* № */}
                <Serif size={16} italic color="var(--muted-2)">
                  {String(rowIdx).padStart(2, "0")}
                </Serif>

                {/* 题目 + ID */}
                <div className="min-w-0">
                  <div className="truncate text-[14px] font-medium text-ink">
                    {a.title}
                  </div>
                  <div className="mt-0.5 block">
                    <Mono size={10}>ID · {a.id.slice(0, 8).toUpperCase()}</Mono>
                  </div>
                </div>

                {/* 类型（用题数替代） */}
                <Serif size={13} italic>
                  {a.questionIds?.length || 0} 题
                </Serif>

                {/* 班级 */}
                <span className="truncate text-[12px] text-muted">
                  {classMap[a.classId] || "—"}
                </span>

                {/* 截止 */}
                <Mono size={11} color="var(--ink)">
                  {formatDue(a.dueDate)}
                </Mono>

                {/* 提交率 */}
                <div className="flex items-center gap-2">
                  <div
                    className="relative flex-1"
                    style={{
                      height: 3,
                      background: "var(--line-soft)",
                    }}
                  >
                    <div
                      className="absolute inset-y-0 left-0"
                      style={{
                        width: `${rate.pct}%`,
                        background: barColor,
                      }}
                    />
                  </div>
                  <Mono size={10}>
                    {rate.active}/{rate.total || "—"}
                  </Mono>
                </div>

                {/* 均分（无聚合 → 占位） */}
                <Numeral size={20}>—</Numeral>

                {/* 状态 Chip */}
                <div className="text-right">
                  <Chip tone={STATUS_META[status].tone}>
                    {STATUS_META[status].label}
                  </Chip>
                </div>
              </button>
            );
          })
        )}
      </div>

      {/* 分页 */}
      {filtered.length > 0 && (
        <div className="mt-3.5 flex items-center justify-between">
          <Mono size={10}>
            显示 {(safePage - 1) * PAGE_SIZE + 1}–
            {Math.min(safePage * PAGE_SIZE, filtered.length)} / 共{" "}
            {filtered.length} 项
          </Mono>
          <Pagination
            page={safePage}
            totalPages={totalPages}
            onChange={setPage}
          />
        </div>
      )}
    </div>
  );
}

// ── 分页器 ───────────────────────────────────────────────────────────

function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (n: number) => void;
}) {
  // 构造页码序列，中间若超 5 页显示省略号
  const pages: Array<number | "…"> = [];
  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i);
  } else {
    pages.push(1, 2, 3, 4, "…", totalPages);
  }

  const btnStyle = (active: boolean, disabled?: boolean) => ({
    padding: "6px 11px",
    border: "1px solid var(--line)",
    background: active ? "var(--ink)" : "var(--ivory)",
    color: active ? "var(--ivory)" : "var(--ink)",
    fontSize: 11,
    fontFamily: "var(--font-mono)",
    cursor: disabled ? "default" : "pointer",
    opacity: disabled ? 0.4 : 1,
  });

  return (
    <div className="flex gap-1">
      <button
        disabled={page <= 1}
        onClick={() => onChange(Math.max(1, page - 1))}
        style={btnStyle(false, page <= 1)}
      >
        ‹
      </button>
      {pages.map((p, i) =>
        p === "…" ? (
          <span key={`d-${i}`} style={btnStyle(false, true)}>
            …
          </span>
        ) : (
          <button
            key={p}
            onClick={() => onChange(p)}
            style={btnStyle(p === page)}
          >
            {p}
          </button>
        )
      )}
      <button
        disabled={page >= totalPages}
        onClick={() => onChange(Math.min(totalPages, page + 1))}
        style={btnStyle(false, page >= totalPages)}
      >
        ›
      </button>
    </div>
  );
}
