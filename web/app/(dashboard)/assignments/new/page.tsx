"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from "@/components/ui/card";
import { Loader2 } from "lucide-react";

interface ClassOption {
  id: string;
  name: string;
  examType: string;
}

interface QuestionOption {
  id: string;
  examType: string;
  section: string;
  topic: string;
  promptText: string;
}

export default function NewAssignmentPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [classId, setClassId] = useState("");
  const [selectedQuestionIds, setSelectedQuestionIds] = useState<string[]>([]);
  const [dueDate, setDueDate] = useState("");

  const [classes, setClasses] = useState<ClassOption[]>([]);
  const [questions, setQuestions] = useState<QuestionOption[]>([]);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [classesData, questionsData] = await Promise.all([
          api.get<ClassOption[]>("/classes"),
          api.get<QuestionOption[]>("/questions"),
        ]);
        setClasses(classesData);
        setQuestions(questionsData);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "加载数据失败");
      } finally {
        setLoadingOptions(false);
      }
    };
    loadOptions();
  }, []);

  const toggleQuestion = (id: string) => {
    setSelectedQuestionIds((prev) =>
      prev.includes(id) ? prev.filter((qid) => qid !== id) : [...prev, id]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!classId) {
      setError("请选择班级");
      return;
    }
    if (selectedQuestionIds.length === 0) {
      setError("请至少选择一道题目");
      return;
    }
    setSubmitting(true);
    setError("");

    try {
      await api.post("/assignments", {
        title,
        description,
        classId,
        questionIds: selectedQuestionIds,
        dueDate: dueDate ? new Date(dueDate).toISOString() : undefined,
      });
      router.push("/assignments");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "创建作业失败");
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingOptions) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-primary">创建作业</h1>
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">加载数据...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">创建作业</h1>

      <Card className="max-w-2xl">
        <form onSubmit={handleSubmit}>
          <CardHeader>
            <CardTitle>作业信息</CardTitle>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="rounded-lg border border-data-red/20 bg-data-red/5 p-3 text-sm text-data-red">
                {error}
              </div>
            )}

            {/* 标题 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">标题</label>
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="作业标题"
                required
              />
            </div>

            {/* 描述 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">描述</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="作业描述（可选）"
                rows={3}
                className="flex w-full rounded-lg border border-border bg-white px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>

            {/* 班级 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">班级</label>
              <select
                value={classId}
                onChange={(e) => setClassId(e.target.value)}
                required
                className="flex h-10 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">请选择班级</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.examType})
                  </option>
                ))}
              </select>
            </div>

            {/* 截止日期 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">截止日期</label>
              <Input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </div>

            {/* 题目选择 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">
                选择题目 ({selectedQuestionIds.length} 已选)
              </label>
              {questions.length === 0 ? (
                <p className="text-sm text-muted-foreground">暂无可用题目，请先在题库中创建题目</p>
              ) : (
                <div className="max-h-64 space-y-2 overflow-y-auto rounded-lg border border-border p-3">
                  {questions.map((q) => (
                    <label
                      key={q.id}
                      className="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-muted/50"
                    >
                      <input
                        type="checkbox"
                        checked={selectedQuestionIds.includes(q.id)}
                        onChange={() => toggleQuestion(q.id)}
                        className="mt-0.5 h-4 w-4 rounded border-border text-accent focus:ring-ring"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-primary">
                          {q.topic || q.promptText}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {q.examType} / {q.section}
                        </p>
                      </div>
                    </label>
                  ))}
                </div>
              )}
            </div>
          </CardContent>

          <CardFooter className="gap-3">
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              创建作业
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push("/assignments")}
            >
              取消
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
