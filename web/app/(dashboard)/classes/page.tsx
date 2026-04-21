"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus, Filter, Pencil, Trash2, X, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";
import {
  EditorialEmptyState,
  EditorialErrorState,
  EditorialSkeleton,
  mapErrorToCode,
} from "@/components/ui/editorial-states";

// ── Types ────────────────────────────────────────────────────────────

interface ClassUser {
  id: string;
  name: string;
  email: string;
}

interface ClassItem {
  id: string;
  name: string;
  examType: "TOEFL" | "IELTS" | "BOTH";
  teacher: ClassUser | null;
  students: ClassUser[] | null;
  createdAt: string;
}

// ── Tabs（目前无 status 字段，仅视觉占位）─────────────────────────
const TABS = ["全部", "进行中", "待开班", "已结课"] as const;
type TabKey = (typeof TABS)[number];

export default function ClassesPage() {
  const router = useRouter();
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [errorObj, setErrorObj] = useState<unknown>(null);
  const [activeTab, setActiveTab] = useState<TabKey>("全部");

  // 新建/编辑表单
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState("");
  const [formExamType, setFormExamType] = useState<ClassItem["examType"]>("IELTS");
  const [creating, setCreating] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");

  const fetchClasses = async () => {
    setLoading(true);
    setErrorObj(null);
    try {
      const data = await api.get<ClassItem[]>("/classes");
      setClasses(data);
    } catch (err) {
      setErrorObj(err);
      setError(err instanceof Error ? err.message : "加载班级列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClasses();
  }, []);

  const totalStudents = useMemo(
    () => classes.reduce((sum, c) => sum + (c.students?.length || 0), 0),
    [classes]
  );

  const filtered = classes; // 目前 API 无 status 字段，Tab 视觉保留；未来接入后在此过滤

  // ── 首次加载 / 加载失败 / 真空态 ───────────────────────────────
  if (loading && classes.length === 0) {
    return <EditorialSkeleton headerTitle="CLASSES · 加载中" cardCount={3} />;
  }
  if (errorObj && classes.length === 0) {
    return (
      <EditorialErrorState code={mapErrorToCode(errorObj)} onRetry={fetchClasses} />
    );
  }
  if (classes.length === 0) {
    return (
      <EditorialEmptyState
        eyebrow="NO CLASSES · 班级空空"
        headline="Start your first"
        headlineItalic="— class."
        message={"还没有创建任何班级。\n创建班级后即可邀请学生加入。"}
        primaryCTA={{
          title: "新建班级",
          onClick: () => setShowForm(true),
        }}
        secondaryCTA={{
          title: "查看作业",
          onClick: () => router.push("/assignments"),
        }}
        footer="EMPTY STATE"
        footerNumber="N° CLASS"
      />
    );
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) return;
    setCreating(true);
    setError("");
    try {
      await api.post("/classes", {
        name: formName.trim(),
        examType: formExamType,
      });
      setFormName("");
      setShowForm(false);
      setLoading(true);
      await fetchClasses();
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建班级失败");
    } finally {
      setCreating(false);
    }
  };

  const handleRename = async (id: string) => {
    if (!editName.trim()) {
      setEditingId(null);
      return;
    }
    await api.put(`/classes/${id}`, { name: editName.trim() });
    setEditingId(null);
    setLoading(true);
    await fetchClasses();
  };

  const handleDelete = async (cls: ClassItem) => {
    if (!confirm(`确认删除班级 "${cls.name}"？此操作不可恢复。`)) return;
    try {
      await api.delete(`/classes/${cls.id}`);
      setLoading(true);
      await fetchClasses();
    } catch {
      alert("删除失败");
    }
  };

  return (
    <div>
      {/* Filter + Actions 行 */}
      <div className="mb-6 flex items-center gap-3 border-b border-line pb-4">
        {/* Tab group */}
        <div className="flex border border-line">
          {TABS.map((t, i) => (
            <button
              key={t}
              onClick={() => setActiveTab(t)}
              className="px-3.5 py-2 text-[12px] transition-colors"
              style={{
                background: activeTab === t ? "var(--ink)" : "transparent",
                color: activeTab === t ? "var(--ivory)" : "var(--ink)",
                borderRight: i < TABS.length - 1 ? "1px solid var(--line)" : 0,
              }}
            >
              {t}
            </button>
          ))}
        </div>
        <Mono size={10} style={{ marginLeft: 8 }}>
          共 {classes.length} 个班级 · {totalStudents} 名学生
        </Mono>
        <div className="flex-1" />
        <HairlineBtn leftIcon={<Filter className="h-[13px] w-[13px]" strokeWidth={1.3} />}>
          筛选
        </HairlineBtn>
        <HairlineBtn
          primary
          onClick={() => setShowForm((v) => !v)}
          leftIcon={
            showForm ? (
              <X className="h-[13px] w-[13px]" strokeWidth={1.5} />
            ) : (
              <Plus className="h-[13px] w-[13px]" strokeWidth={1.5} />
            )
          }
        >
          {showForm ? "取消" : "新建班级"}
        </HairlineBtn>
      </div>

      {/* 内联创建表单 */}
      {showForm && (
        <form
          onSubmit={handleCreate}
          className="mb-6 flex flex-wrap items-end gap-4 border border-line bg-ivory p-5"
        >
          <div className="min-w-[220px] flex-1">
            <Eyebrow>班级名称</Eyebrow>
            <input
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="如：雅思冲刺 · 周末班"
              required
              className="mt-1.5 w-full border-0 border-b border-ink bg-transparent pb-1 text-[14px] text-ink outline-none"
            />
          </div>
          <div>
            <Eyebrow>考试类型</Eyebrow>
            <select
              value={formExamType}
              onChange={(e) => setFormExamType(e.target.value as ClassItem["examType"])}
              className="mt-1.5 border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
            >
              <option value="IELTS">IELTS</option>
              <option value="TOEFL">TOEFL</option>
              <option value="BOTH">BOTH</option>
            </select>
          </div>
          <HairlineBtn
            primary
            type="submit"
            disabled={creating}
            leftIcon={creating ? <Loader2 className="h-[13px] w-[13px] animate-spin" /> : undefined}
          >
            确认创建
          </HairlineBtn>
        </form>
      )}

      {/* 创建失败等局部错误条（不影响已渲染列表） */}
      {error && (
        <div
          className="mb-6 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      {/* List */}
      {filtered.length === 0 ? (
        <div className="border border-line bg-ivory py-16 text-center">
          <Mono size={11}>— 当前筛选无结果 —</Mono>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {filtered.map((cls, idx) => (
            <ClassCard
              key={cls.id}
              cls={cls}
              index={idx}
              isEditing={editingId === cls.id}
              editValue={editName}
              onEditChange={setEditName}
              onStartEdit={(c) => {
                setEditingId(c.id);
                setEditName(c.name);
              }}
              onCommitEdit={() => handleRename(cls.id)}
              onCancelEdit={() => setEditingId(null)}
              onDelete={() => handleDelete(cls)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Card ─────────────────────────────────────────────────────────────

interface ClassCardProps {
  cls: ClassItem;
  index: number;
  isEditing: boolean;
  editValue: string;
  onEditChange: (v: string) => void;
  onStartEdit: (c: ClassItem) => void;
  onCommitEdit: () => void;
  onCancelEdit: () => void;
  onDelete: () => void;
}

function ClassCard({
  cls,
  index,
  isEditing,
  editValue,
  onEditChange,
  onStartEdit,
  onCommitEdit,
  onCancelEdit,
  onDelete,
}: ClassCardProps) {
  const num = String(index + 1).padStart(2, "0");
  const code = cls.id.slice(0, 8).toUpperCase(); // 截取 uuid 前 8 位作显示码
  const studentCount = cls.students?.length || 0;

  // avg / target / examDate 字段后端暂缺，用占位
  const avg: string = "—";
  const target: string = "—";
  const examDate: string = ""; // 留空则不显示考试日期

  return (
    <div className="relative border border-line bg-ivory px-6 py-5">
      {/* 顶部：编号 + 状态 + 名称 + 编辑/删除菜单 */}
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2.5">
            <Serif size={22} italic color="var(--accent)">
              №{num}
            </Serif>
            <Chip tone="moss">进行中</Chip>
            <Chip tone="muted">{cls.examType}</Chip>
          </div>
          <div className="mt-2.5">
            {isEditing ? (
              <input
                value={editValue}
                onChange={(e) => onEditChange(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") onCommitEdit();
                  if (e.key === "Escape") onCancelEdit();
                }}
                onBlur={onCommitEdit}
                autoFocus
                className="w-full border-0 border-b border-ink bg-transparent pb-1 font-serif text-[22px] text-ink outline-none"
                style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
              />
            ) : (
              <Link href={`/classes/${cls.id}/students`}>
                <Serif size={22} className="hover:underline">
                  {cls.name}
                </Serif>
              </Link>
            )}
          </div>
          <div className="mt-1 block">
            <Mono size={10}>CODE · {code}</Mono>
            {cls.teacher?.name && (
              <Mono size={10} style={{ marginLeft: 12 }}>
                TEACHER · {cls.teacher.name}
              </Mono>
            )}
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button
            aria-label="编辑"
            onClick={() => onStartEdit(cls)}
            className="p-1.5 text-muted transition-colors hover:text-ink"
          >
            <Pencil className="h-4 w-4" strokeWidth={1.3} />
          </button>
          <button
            aria-label="删除"
            onClick={onDelete}
            className="p-1.5 text-muted transition-colors hover:text-accent"
          >
            <Trash2 className="h-4 w-4" strokeWidth={1.3} />
          </button>
        </div>
      </div>

      {/* 三列小统计 */}
      <div className="mt-5 grid grid-cols-3 border-t border-line pt-3.5">
        <div>
          <Eyebrow>学生</Eyebrow>
          <div className="mt-1">
            <Numeral size={26}>{studentCount}</Numeral>
          </div>
        </div>
        <div className="pl-3.5" style={{ borderLeft: "1px solid var(--line)" }}>
          <Eyebrow>均分</Eyebrow>
          <div className="mt-1">
            <Numeral size={26}>{avg}</Numeral>
            <span className="ml-1 text-[11px] text-muted">/ 目标 {target}</span>
          </div>
        </div>
        <div className="pl-3.5" style={{ borderLeft: "1px solid var(--line)" }}>
          <Eyebrow>考试</Eyebrow>
          <div className="mt-1">
            {examDate ? (
              <>
                <Serif size={15}>{examDate.slice(5)}</Serif>
                <div className="block">
                  <Mono size={10}>{examDate.slice(0, 4)}</Mono>
                </div>
              </>
            ) : (
              <Mono size={11}>— 未设定 —</Mono>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
