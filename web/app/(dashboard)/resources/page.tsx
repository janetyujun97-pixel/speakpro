"use client";

import { useEffect, useMemo, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Plus,
  Upload,
  Pencil,
  Trash2,
  MoreHorizontal,
  Music,
  FileText,
  Video,
  BookOpen,
} from "lucide-react";
import { api } from "@/lib/api";
import FileUpload from "@/components/ui/file-upload";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";

// ── 类型 ────────────────────────────────────────────────────────────

interface Question {
  id: string;
  examType: string;
  section: string;
  topic: string;
  promptText: string;
  difficulty: number;
  tags: string[];
  createdAt: string;
}

interface ResourceItem {
  id: string;
  title: string;
  type: string; // audio | document | video | wordlist
  fileUrl: string;
  fileSize: number;
  examType: string;
  tags: string[];
  createdAt: string;
}

type TabKey = "questions" | "library";

const RESOURCE_TYPES: Array<{
  value: string;
  label: string;
  icon: typeof Music;
}> = [
  { value: "audio",    label: "音频", icon: Music     },
  { value: "document", label: "文档", icon: FileText  },
  { value: "video",    label: "视频", icon: Video     },
  { value: "wordlist", label: "词表", icon: BookOpen  },
];

// ── 入口组件（外包 Suspense 供 useSearchParams 使用） ─────────────

export default function ResourcesPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center"><Mono size={11}>— 加载中 —</Mono></div>}>
      <ResourcesInner />
    </Suspense>
  );
}

function ResourcesInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialTab = (searchParams.get("tab") as TabKey) || "questions";
  const [tab, setTab] = useState<TabKey>(
    initialTab === "library" ? "library" : "questions"
  );

  const switchTab = (t: TabKey) => {
    setTab(t);
    // 同步 URL，便于直接分享 tab
    const p = new URLSearchParams(searchParams.toString());
    p.set("tab", t);
    router.replace(`/resources?${p.toString()}`);
  };

  return (
    <div>
      {/* 大 tab 切换 */}
      <div className="mb-7 flex items-center border-b border-line pb-3">
        <div className="flex items-baseline gap-7">
          {(
            [
              { key: "questions", label: "题库",     en: "Questions" },
              { key: "library",   label: "教学资源", en: "Library"   },
            ] as const
          ).map((t) => {
            const on = tab === t.key;
            return (
              <button
                key={t.key}
                onClick={() => switchTab(t.key)}
                className="flex items-baseline gap-2 pb-1 transition-colors"
                style={{
                  borderBottom: `1.5px solid ${on ? "var(--accent)" : "transparent"}`,
                }}
              >
                <Serif size={22} color={on ? "var(--ink)" : "var(--muted-2)"}>
                  {t.label}
                </Serif>
                <Serif size={13} italic color={on ? "var(--accent)" : "var(--muted-2)"}>
                  {t.en}
                </Serif>
              </button>
            );
          })}
        </div>
      </div>

      {tab === "questions" ? <QuestionsPanel /> : <LibraryPanel />}
    </div>
  );
}

// ── 题库面板 ──────────────────────────────────────────────────────

function QuestionsPanel() {
  const [all, setAll] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [category, setCategory] = useState<string>("全部");
  const [deleting, setDeleting] = useState<string | null>(null);

  const fetchAll = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await api.get<Question[] | { items: Question[] }>(
        "/questions"
      );
      const list = Array.isArray(data) ? data : data.items || [];
      setAll(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载题目失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  // 按 section 分组统计
  const stats = useMemo(() => {
    const groups: Record<string, number> = {};
    for (const q of all) {
      const k = q.section || "—";
      groups[k] = (groups[k] || 0) + 1;
    }
    const top3 = ["Part 1", "Part 2", "Part 3"];
    const counts = top3.map((k) => ({ lbl: k, n: groups[k] || 0 }));
    const otherTotal = Object.entries(groups)
      .filter(([k]) => !top3.includes(k))
      .reduce((sum, [, v]) => sum + v, 0);
    return [...counts, { lbl: "模考 · 朗读 · 跟读", n: otherTotal }];
  }, [all]);

  const CATEGORIES = [
    "全部",
    ...Array.from(new Set(all.map((q) => q.section))).filter(Boolean),
  ];

  const filtered = useMemo(() => {
    if (category === "全部") return all;
    return all.filter((q) => q.section === category);
  }, [all, category]);

  const handleDelete = async (id: string) => {
    if (!confirm("确定要删除这道题目吗？")) return;
    setDeleting(id);
    try {
      await api.delete(`/questions/${id}`);
      setAll((prev) => prev.filter((q) => q.id !== id));
    } catch (err) {
      alert(err instanceof Error ? err.message : "删除失败");
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div>
      {/* 分段统计 */}
      <div className="mb-7 grid grid-cols-4 gap-4">
        {stats.map((c, i) => (
          <div
            key={c.lbl}
            className="bg-ivory px-5 py-4"
            style={{
              border: "1px solid var(--line)",
              borderLeft: i === 0 ? "3px solid var(--accent)" : "1px solid var(--line)",
            }}
          >
            <Eyebrow>{c.lbl}</Eyebrow>
            <div className="mt-1.5 flex items-baseline gap-1.5">
              <Numeral size={36}>{c.n}</Numeral>
              <span className="text-[11px] text-muted">题</span>
            </div>
            <Serif size={12} italic color="var(--muted)">
              {i === 0 ? "Warm-up" : i === 1 ? "Long-turn" : i === 2 ? "Discussion" : "Other"}
            </Serif>
          </div>
        ))}
      </div>

      {/* 过滤条 */}
      <div className="mb-4 flex items-center gap-2.5">
        <Eyebrow>CATEGORY</Eyebrow>
        {CATEGORIES.map((t) => {
          const on = category === t;
          return (
            <button
              key={t}
              onClick={() => setCategory(t)}
              className="border border-line px-2.5 py-1 text-[11px] transition-colors"
              style={{
                background: on ? "var(--ink)" : "transparent",
                color: on ? "var(--ivory)" : "var(--ink)",
                borderRadius: 2,
              }}
            >
              {t}
            </button>
          );
        })}
        <div className="flex-1" />
        <HairlineBtn leftIcon={<Upload className="h-[13px] w-[13px]" strokeWidth={1.3} />}>
          批量导入
        </HairlineBtn>
        <Link href="/resources/questions/new">
          <HairlineBtn primary leftIcon={<Plus className="h-[13px] w-[13px]" strokeWidth={1.5} />}>
            新建题目
          </HairlineBtn>
        </Link>
      </div>

      {/* 错误 / 空 / 列表 */}
      {error && (
        <div
          className="mb-6 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-16 text-center">
          <Mono size={11}>— 加载中 —</Mono>
        </div>
      ) : filtered.length === 0 ? (
        <div className="border border-line bg-ivory py-16 text-center">
          <Mono size={11}>— 暂无题目 —</Mono>
        </div>
      ) : (
        <div className="border border-line bg-ivory">
          {filtered.map((q, i) => (
            <QuestionRow
              key={q.id}
              q={q}
              index={i}
              total={filtered.length}
              onDelete={() => handleDelete(q.id)}
              deleting={deleting === q.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function QuestionRow({
  q,
  index,
  total,
  onDelete,
  deleting,
}: {
  q: Question;
  index: number;
  total: number;
  onDelete: () => void;
  deleting: boolean;
}) {
  const diff = ["", "易", "易", "中", "难", "难"][q.difficulty] || "—";
  return (
    <div
      className="grid items-center gap-4"
      style={{
        gridTemplateColumns: "90px 80px 1fr 100px 80px 70px 110px",
        padding: "18px 24px",
        borderBottom: index < total - 1 ? "1px solid var(--line)" : 0,
      }}
    >
      <Serif size={17} italic color="var(--accent)">
        N°{String(index + 1).padStart(3, "0")}
      </Serif>
      <Chip tone="muted">{q.section || "—"}</Chip>
      <div className="min-w-0">
        <div
          className="truncate font-serif text-[14px] text-ink"
          style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
        >
          {q.topic || q.promptText}
        </div>
        <div className="mt-1 flex items-center gap-2">
          {(q.tags || []).slice(0, 2).map((tag) => (
            <Chip key={tag} tone="muted">
              {tag}
            </Chip>
          ))}
          <Mono size={10}>ID · {q.id.slice(0, 8).toUpperCase()}</Mono>
        </div>
      </div>
      <div>
        <Mono size={10}>USED</Mono>
        <div>
          <Numeral size={17}>—</Numeral>
        </div>
      </div>
      <div>
        <Mono size={10}>AVG</Mono>
        <div>
          <Numeral size={17} color="var(--accent)">—</Numeral>
        </div>
      </div>
      <Serif size={13} italic color="var(--muted)">
        {diff}
      </Serif>
      <div className="flex justify-end gap-1.5">
        <Link
          href={`/resources/questions/${q.id}/edit`}
          className="inline-flex h-7 w-7 items-center justify-center border border-line transition-colors hover:bg-bg-soft"
          aria-label="编辑"
        >
          <Pencil className="h-[13px] w-[13px] text-muted" strokeWidth={1.3} />
        </Link>
        <button
          onClick={onDelete}
          disabled={deleting}
          className="inline-flex h-7 w-7 items-center justify-center border border-line transition-colors hover:text-accent disabled:opacity-50"
          aria-label="删除"
        >
          <Trash2 className="h-[13px] w-[13px] text-muted" strokeWidth={1.3} />
        </button>
        <button
          className="inline-flex h-7 w-7 items-center justify-center border border-line transition-colors hover:bg-bg-soft"
          aria-label="更多"
        >
          <MoreHorizontal className="h-[13px] w-[13px] text-muted" strokeWidth={1.3} />
        </button>
      </div>
    </div>
  );
}

// ── 教学资源面板 ─────────────────────────────────────────────────

function LibraryPanel() {
  const [items, setItems] = useState<ResourceItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<string>("");
  const [showUpload, setShowUpload] = useState(false);

  // 新资源表单
  const [newTitle, setNewTitle] = useState("");
  const [newType, setNewType] = useState("audio");
  const [newExamType, setNewExamType] = useState("IELTS");
  const [uploadedUrl, setUploadedUrl] = useState("");

  useEffect(() => {
    loadResources();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter]);

  async function loadResources() {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (typeFilter) params.set("type", typeFilter);
      const data = await api.get<ResourceItem[]>(
        `/resources${params.toString() ? "?" + params : ""}`
      );
      setItems(Array.isArray(data) ? data : []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate() {
    if (!newTitle || !uploadedUrl) return;
    try {
      await api.post("/resources", {
        title: newTitle,
        type: newType,
        fileUrl: uploadedUrl,
        examType: newExamType,
      });
      setShowUpload(false);
      setNewTitle("");
      setUploadedUrl("");
      loadResources();
    } catch {
      alert("创建资源失败");
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("确定要删除该资源吗？")) return;
    try {
      await api.delete(`/resources/${id}`);
      setItems((prev) => prev.filter((r) => r.id !== id));
    } catch {
      alert("删除失败");
    }
  }

  function formatSize(bytes: number) {
    if (!bytes) return "—";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return (
    <div>
      {/* 过滤 + 上传 */}
      <div className="mb-4 flex items-center gap-2.5">
        <Eyebrow>TYPE</Eyebrow>
        {[{ value: "", label: "全部" }, ...RESOURCE_TYPES].map((t) => {
          const on = typeFilter === t.value;
          return (
            <button
              key={t.value || "all"}
              onClick={() => setTypeFilter(t.value)}
              className="border border-line px-2.5 py-1 text-[11px] transition-colors"
              style={{
                background: on ? "var(--ink)" : "transparent",
                color: on ? "var(--ivory)" : "var(--ink)",
                borderRadius: 2,
              }}
            >
              {t.label}
            </button>
          );
        })}
        <div className="flex-1" />
        <HairlineBtn
          primary
          onClick={() => setShowUpload((v) => !v)}
          leftIcon={
            <Plus
              className="h-[13px] w-[13px]"
              strokeWidth={1.5}
              style={{ transform: showUpload ? "rotate(45deg)" : "none" }}
            />
          }
        >
          {showUpload ? "取消上传" : "上传资源"}
        </HairlineBtn>
      </div>

      {/* 上传表单 */}
      {showUpload && (
        <div className="mb-6 border border-line bg-ivory p-5">
          <Eyebrow>上传新资源</Eyebrow>
          <div className="mt-3 grid grid-cols-3 gap-4">
            <div>
              <label className="mb-1.5 block">
                <Mono size={10}>标题</Mono>
              </label>
              <input
                type="text"
                placeholder="资源标题"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="w-full border-0 border-b border-ink bg-transparent pb-1 text-[14px] text-ink outline-none"
              />
            </div>
            <div>
              <label className="mb-1.5 block">
                <Mono size={10}>类型</Mono>
              </label>
              <select
                value={newType}
                onChange={(e) => setNewType(e.target.value)}
                className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
              >
                {RESOURCE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block">
                <Mono size={10}>考试类型</Mono>
              </label>
              <select
                value={newExamType}
                onChange={(e) => setNewExamType(e.target.value)}
                className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
              >
                <option value="IELTS">IELTS</option>
                <option value="TOEFL">TOEFL</option>
              </select>
            </div>
          </div>
          <div className="mt-4">
            <FileUpload
              accept={
                newType === "audio"
                  ? "audio/*"
                  : newType === "video"
                  ? "video/*"
                  : "*"
              }
              maxSizeMB={50}
              onUploadComplete={(url: string) => setUploadedUrl(url)}
              onError={(err: string) => alert(err)}
            />
          </div>
          {uploadedUrl && (
            <div className="mt-3 flex justify-end">
              <HairlineBtn primary onClick={handleCreate} disabled={!newTitle}>
                确认创建
              </HairlineBtn>
            </div>
          )}
        </div>
      )}

      {/* 列表 */}
      {loading ? (
        <div className="py-16 text-center">
          <Mono size={11}>— 加载中 —</Mono>
        </div>
      ) : items.length === 0 ? (
        <div className="border border-line bg-ivory py-16 text-center">
          <Mono size={11}>— 暂无资源，点击「上传资源」开始 —</Mono>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {items.map((r) => {
            const meta = RESOURCE_TYPES.find((t) => t.value === r.type);
            const Icon = meta?.icon || FileText;
            return (
              <div
                key={r.id}
                className="border border-line bg-ivory p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-3">
                    <div
                      className="flex h-10 w-10 shrink-0 items-center justify-center border border-line"
                      style={{ background: "var(--accent-soft)" }}
                    >
                      <Icon
                        className="h-[18px] w-[18px]"
                        strokeWidth={1.3}
                        style={{ color: "var(--accent)" }}
                      />
                    </div>
                    <div className="min-w-0">
                      <div className="truncate text-[14px] font-medium text-ink">
                        {r.title}
                      </div>
                      <div className="mt-1 flex items-center gap-2">
                        <Chip tone="muted">{meta?.label || r.type}</Chip>
                        <Mono size={10}>
                          {r.examType} · {formatSize(r.fileSize)}
                        </Mono>
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={() => handleDelete(r.id)}
                    className="inline-flex h-7 w-7 shrink-0 items-center justify-center border border-line transition-colors hover:text-accent"
                    aria-label="删除"
                  >
                    <Trash2 className="h-[13px] w-[13px] text-muted" strokeWidth={1.3} />
                  </button>
                </div>
                {r.tags?.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {r.tags.slice(0, 3).map((tag) => (
                      <Chip key={tag} tone="muted">
                        {tag}
                      </Chip>
                    ))}
                  </div>
                )}
                <div className="mt-3">
                  <Mono size={10}>
                    {new Date(r.createdAt).toLocaleDateString("zh-CN")}
                  </Mono>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
