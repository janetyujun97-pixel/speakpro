"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import {
  UserPlus,
  UserMinus,
  ArrowLeft,
  X,
  Loader2,
} from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";

interface ClassDetail {
  id: string;
  name: string;
  examType: string;
  students: Student[];
}

interface Student {
  id: string;
  name: string;
  email: string;
}

const GRID_COLS = "40px 1.3fr 1fr 120px";

export default function ClassStudentsPage() {
  const { id: classId } = useParams() as { id: string };
  const [classData, setClassData] = useState<ClassDetail | null>(null);
  const [allStudents, setAllStudents] = useState<Student[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId]);

  async function loadData() {
    try {
      const [cls, students] = await Promise.all([
        api.get<ClassDetail>(`/classes/${classId}`),
        api.get<Student[]>("/users?role=student"),
      ]);
      setClassData(cls);
      setAllStudents(students);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function addStudent() {
    if (!selectedStudentId) return;
    setSubmitting(true);
    setError("");
    try {
      await api.post(`/classes/${classId}/students`, {
        studentId: selectedStudentId,
      });
      setSelectedStudentId("");
      setShowAddForm(false);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "添加学生失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function removeStudent(studentId: string, name: string) {
    if (!confirm(`确认将 ${name} 移出班级？`)) return;
    try {
      await api.delete(`/classes/${classId}/students/${studentId}`);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "移除学生失败");
    }
  }

  if (loading) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 加载中 —</Mono>
      </div>
    );
  }

  if (!classData) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 班级不存在 —</Mono>
      </div>
    );
  }

  const currentStudentIds = new Set(classData.students.map((s) => s.id));
  const availableStudents = allStudents.filter(
    (s) => !currentStudentIds.has(s.id)
  );
  const studentCount = classData.students.length;

  return (
    <div>
      {/* ── 班级标题栏 ─────────────────────────────── */}
      <div className="mb-5 flex items-start justify-between gap-4 border-b border-line pb-5">
        <div className="min-w-0 flex-1">
          <div className="mb-1.5">
            <Eyebrow>CLASS · 学生管理</Eyebrow>
          </div>
          <Serif size={26}>{classData.name}</Serif>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Chip tone="ink">{classData.examType}</Chip>
            <Chip tone="muted">{studentCount} 名学生</Chip>
            <Mono size={10}>
              CODE · {classData.id.slice(0, 8).toUpperCase()}
            </Mono>
          </div>
        </div>
        <Link href="/classes">
          <HairlineBtn
            leftIcon={
              <ArrowLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />
            }
          >
            返回班级
          </HairlineBtn>
        </Link>
      </div>

      {/* ── 错误条 ─────────────────────────────── */}
      {error && (
        <div
          className="mb-5 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      {/* ── 操作栏 ─────────────────────────────── */}
      <div className="mb-5 flex items-center gap-3">
        <Mono size={10}>成员名册 · ROSTER</Mono>
        <div className="flex-1" />
        <HairlineBtn
          primary
          onClick={() => {
            setShowAddForm((v) => !v);
            setSelectedStudentId("");
          }}
          leftIcon={
            showAddForm ? (
              <X className="h-[13px] w-[13px]" strokeWidth={1.5} />
            ) : (
              <UserPlus className="h-[13px] w-[13px]" strokeWidth={1.5} />
            )
          }
        >
          {showAddForm ? "取消" : "添加学生"}
        </HairlineBtn>
      </div>

      {/* ── 添加学生表单（内联）─────────────────── */}
      {showAddForm && (
        <div className="mb-5 flex flex-wrap items-end gap-4 border border-line bg-ivory p-5">
          <div className="min-w-[260px] flex-1">
            <Eyebrow>选择学生</Eyebrow>
            {availableStudents.length === 0 ? (
              <div className="mt-2.5">
                <Mono size={11}>— 全部学生已在本班 —</Mono>
              </div>
            ) : (
              <select
                value={selectedStudentId}
                onChange={(e) => setSelectedStudentId(e.target.value)}
                className="mt-1.5 w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
              >
                <option value="">— 请选择 —</option>
                {availableStudents.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}（{s.email}）
                  </option>
                ))}
              </select>
            )}
          </div>
          <HairlineBtn
            primary
            onClick={addStudent}
            disabled={!selectedStudentId || submitting}
            leftIcon={
              submitting ? (
                <Loader2 className="h-[13px] w-[13px] animate-spin" />
              ) : undefined
            }
          >
            {submitting ? "添加中…" : "确认添加"}
          </HairlineBtn>
        </div>
      )}

      {/* ── 学生列表 ─────────────────────────────── */}
      <div className="rounded-xs border border-line bg-ivory">
        {/* 表头 */}
        <div
          className="grid items-center gap-2"
          style={{
            gridTemplateColumns: GRID_COLS,
            padding: "14px 22px",
            background: "var(--bg-soft)",
            borderBottom: "1px solid var(--line)",
          }}
        >
          {["№", "姓名", "邮箱", ""].map((h, i) => (
            <Eyebrow key={i} style={{ fontSize: 9 }}>
              {h}
            </Eyebrow>
          ))}
        </div>

        {/* 行 */}
        {classData.students.length === 0 ? (
          <div className="py-16 text-center">
            <Mono size={11}>— 暂无学生，点击右上方"添加学生" —</Mono>
          </div>
        ) : (
          classData.students.map((student, idx) => (
            <div
              key={student.id}
              className="grid items-center gap-2 transition-colors hover:bg-bg-soft/50"
              style={{
                gridTemplateColumns: GRID_COLS,
                padding: "16px 22px",
                borderBottom:
                  idx < classData.students.length - 1
                    ? "1px solid var(--line)"
                    : 0,
              }}
            >
              {/* № */}
              <Serif size={15} italic color="var(--muted-2)">
                {String(idx + 1).padStart(2, "0")}
              </Serif>

              {/* 姓名（可点击查看详情） */}
              <Link
                href={`/classes/${classId}/students/${student.id}`}
                className="min-w-0 text-[14px] font-medium text-ink hover:underline"
              >
                {student.name}
              </Link>

              {/* 邮箱 */}
              <Mono size={11} color="var(--muted)">
                {student.email}
              </Mono>

              {/* 移除 */}
              <div className="text-right">
                <button
                  onClick={() => removeStudent(student.id, student.name)}
                  className="inline-flex items-center gap-1 px-2 py-1 text-[11px] text-muted transition-colors hover:text-accent"
                  aria-label={`移除 ${student.name}`}
                >
                  <UserMinus
                    className="h-[13px] w-[13px]"
                    strokeWidth={1.3}
                  />
                  移除
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* 底部统计 */}
      {studentCount > 0 && (
        <div className="mt-3.5 flex items-center justify-between">
          <Mono size={10}>
            共 <Numeral size={11}>{studentCount}</Numeral> 名学生
          </Mono>
          <Mono size={10}>ROSTER · {classData.examType}</Mono>
        </div>
      )}
    </div>
  );
}
