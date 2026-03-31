"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Plus, Trash2, Loader2 } from "lucide-react";

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

const EXAM_OPTIONS = [
  { value: "", label: "全部考试" },
  { value: "TOEFL", label: "TOEFL" },
  { value: "IELTS", label: "IELTS" },
];

const difficultyLabel = (d: number) => {
  const labels = ["", "入门", "基础", "中等", "进阶", "挑战"];
  return labels[d] || `${d}`;
};

export default function QuestionsPage() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [examType, setExamType] = useState("");
  const [section, setSection] = useState("");
  const [deleting, setDeleting] = useState<string | null>(null);

  const fetchQuestions = async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (examType) params.set("exam_type", examType);
      if (section) params.set("section", section);
      const qs = params.toString();
      const data = await api.get<Question[]>(`/questions${qs ? `?${qs}` : ""}`);
      setQuestions(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "加载题目失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQuestions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [examType, section]);

  const handleDelete = async (id: string) => {
    if (!confirm("确定要删除这道题目吗？")) return;
    setDeleting(id);
    try {
      await api.delete(`/questions/${id}`);
      setQuestions((prev) => prev.filter((q) => q.id !== id));
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "删除失败");
    } finally {
      setDeleting(null);
    }
  };

  // 从已有题目中提取所有 section 选项
  const sectionOptions = Array.from(new Set(questions.map((q) => q.section))).filter(Boolean);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-primary">题库管理</h1>
        <Link href="/resources/questions/new">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            新建题目
          </Button>
        </Link>
      </div>

      {/* 筛选栏 */}
      <div className="flex flex-wrap items-center gap-4">
        <select
          value={examType}
          onChange={(e) => setExamType(e.target.value)}
          className="h-10 rounded-lg border border-border bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          {EXAM_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <select
          value={section}
          onChange={(e) => setSection(e.target.value)}
          className="h-10 rounded-lg border border-border bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">全部题型</option>
          {sectionOptions.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="rounded-lg border border-data-red/20 bg-data-red/5 p-4 text-sm text-data-red">
          {error}
        </div>
      )}

      {/* 加载状态 */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">加载中...</span>
        </div>
      ) : questions.length === 0 ? (
        <div className="rounded-xl border border-border bg-white p-12 text-center text-muted-foreground">
          暂无题目数据
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border bg-white">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">考试类型</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">题型</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">题目</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">难度</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">标签</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">操作</th>
              </tr>
            </thead>
            <tbody>
              {questions.map((q) => (
                <tr key={q.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                  <td className="px-4 py-3">
                    <span className="inline-block rounded bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
                      {q.examType}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-primary">{q.section}</td>
                  <td className="max-w-xs truncate px-4 py-3 text-primary" title={q.promptText}>
                    {q.topic || q.promptText}
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-muted-foreground">{difficultyLabel(q.difficulty)}</span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {(q.tags || []).slice(0, 3).map((tag) => (
                        <span
                          key={tag}
                          className="inline-block rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(q.id)}
                      disabled={deleting === q.id}
                      className="text-data-red hover:text-data-red"
                    >
                      {deleting === q.id ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="h-4 w-4" />
                      )}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
