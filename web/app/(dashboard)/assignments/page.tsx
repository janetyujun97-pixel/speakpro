"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Plus, ClipboardCheck } from "lucide-react";
import {
  EditorialEmptyState,
  EditorialErrorState,
  EditorialSkeleton,
  mapErrorToCode,
} from "@/components/ui/editorial-states";

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

// 根据 classId 简单显示（后续可关联班级名称）
function formatDate(dateStr: string) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return d.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

function submissionSummary(submissions: Submission[] | null) {
  if (!submissions || submissions.length === 0) return "0 份提交";
  const graded = submissions.filter((s) => s.status === "graded").length;
  const submitted = submissions.filter((s) => s.status === "submitted").length;
  const total = submissions.length;
  return `${total} 份 (已批 ${graded}, 待批 ${submitted})`;
}

export default function AssignmentsPage() {
  const router = useRouter();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  const fetchAssignments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<Assignment[]>("/assignments");
      setAssignments(data);
    } catch (err: unknown) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-primary">作业列表</h1>
        <Link href="/assignments/new">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            创建作业
          </Button>
        </Link>
      </div>

      {loading ? (
        <EditorialSkeleton
          headerTitle="ASSIGNMENTS · 加载中"
          cardCount={3}
        />
      ) : error ? (
        <EditorialErrorState
          code={mapErrorToCode(error)}
          onRetry={fetchAssignments}
        />
      ) : assignments.length === 0 ? (
        <EditorialEmptyState
          eyebrow="NO ASSIGNMENTS · 作业空空"
          headline="All caught up,"
          headlineItalic="— for now."
          message={"还没有布置任何作业。\n创建第一个作业开始教学闭环。"}
          primaryCTA={{
            title: "创建作业",
            onClick: () => router.push("/assignments/new"),
          }}
          footer="EMPTY STATE"
          footerNumber="N° ASSIGN"
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border bg-white">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">标题</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">题目数</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">截止日期</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">提交情况</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">操作</th>
              </tr>
            </thead>
            <tbody>
              {assignments.map((a) => (
                <tr key={a.id} className="border-b border-border last:border-0 hover:bg-muted/30">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-primary">{a.title}</p>
                      {a.description && (
                        <p className="mt-0.5 max-w-md truncate text-xs text-muted-foreground">
                          {a.description}
                        </p>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {a.questionIds?.length || 0} 题
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        new Date(a.dueDate) < new Date()
                          ? "text-data-red"
                          : "text-muted-foreground"
                      }
                    >
                      {formatDate(a.dueDate)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {submissionSummary(a.submissions)}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link href={`/assignments/${a.id}/grade`}>
                      <button className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors">
                        <ClipboardCheck className="h-3.5 w-3.5" />
                        批改
                      </button>
                    </Link>
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
