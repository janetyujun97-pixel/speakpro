"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Plus, Users, Loader2, X, Pencil, Trash2 } from "lucide-react";

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

export default function ClassesPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 内联创建表单
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState("");
  const [formExamType, setFormExamType] = useState<string>("TOEFL");
  const [creating, setCreating] = useState(false);

  // 编辑状态
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");

  const fetchClasses = async () => {
    try {
      const data = await api.get<ClassItem[]>("/classes");
      setClasses(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "加载班级列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClasses();
  }, []);

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
      // 重新加载列表
      setLoading(true);
      await fetchClasses();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "创建班级失败");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-primary">班级管理</h1>
        <Button onClick={() => setShowForm((v) => !v)}>
          {showForm ? (
            <>
              <X className="mr-2 h-4 w-4" />
              取消
            </>
          ) : (
            <>
              <Plus className="mr-2 h-4 w-4" />
              创建班级
            </>
          )}
        </Button>
      </div>

      {/* 内联创建表单 */}
      {showForm && (
        <Card>
          <CardContent className="p-4">
            <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-4">
              <div className="min-w-[200px] flex-1 space-y-1.5">
                <label className="text-sm font-medium text-primary">班级名称</label>
                <Input
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="输入班级名称"
                  required
                />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-primary">考试类型</label>
                <select
                  value={formExamType}
                  onChange={(e) => setFormExamType(e.target.value)}
                  className="flex h-10 rounded-lg border border-border bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  <option value="TOEFL">TOEFL</option>
                  <option value="IELTS">IELTS</option>
                  <option value="BOTH">BOTH</option>
                </select>
              </div>
              <Button type="submit" disabled={creating}>
                {creating && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                确认创建
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {error && (
        <div className="rounded-lg border border-data-red/20 bg-data-red/5 p-4 text-sm text-data-red">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">加载中...</span>
        </div>
      ) : classes.length === 0 ? (
        <div className="rounded-xl border border-border bg-white p-12 text-center text-muted-foreground">
          暂无班级数据，点击上方按钮创建班级
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((cls) => (
            <Card key={cls.id} className="transition-shadow hover:shadow-md">
              <CardContent className="p-5">
                <div className="flex items-start justify-between">
                  <Link href={`/classes/${cls.id}/students`} className="space-y-1 flex-1">
                    {editingId === cls.id ? (
                      <input
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        onKeyDown={async (e) => {
                          if (e.key === "Enter" && editName.trim()) {
                            await api.put(`/classes/${cls.id}`, { name: editName.trim() });
                            setEditingId(null);
                            setLoading(true);
                            await fetchClasses();
                          }
                          if (e.key === "Escape") setEditingId(null);
                        }}
                        onBlur={() => setEditingId(null)}
                        autoFocus
                        className="text-base font-semibold border-b-2 border-blue-500 outline-none bg-transparent"
                        onClick={(e) => e.preventDefault()}
                      />
                    ) : (
                      <h3 className="text-base font-semibold text-primary">{cls.name}</h3>
                    )}
                    <span className="inline-block rounded bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                      {cls.examType}
                    </span>
                  </Link>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={(e) => {
                        e.preventDefault();
                        setEditingId(cls.id);
                        setEditName(cls.name);
                      }}
                      className="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                      title="编辑"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={async (e) => {
                        e.preventDefault();
                        if (!confirm(`确认删除班级 "${cls.name}"？此操作不可恢复。`)) return;
                        try {
                          await api.delete(`/classes/${cls.id}`);
                          setLoading(true);
                          await fetchClasses();
                        } catch {
                          alert("删除失败");
                        }
                      }}
                      className="p-1.5 text-gray-400 hover:text-red-600 rounded"
                      title="删除"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                <Link href={`/classes/${cls.id}/students`}>
                  <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Users className="h-4 w-4" />
                      {cls.students?.length || 0} 名学生
                    </span>
                    <span>{cls.teacher?.name || "未分配教师"}</span>
                  </div>
                </Link>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
