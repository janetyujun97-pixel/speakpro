"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from "@/components/ui/card";
import { Loader2 } from "lucide-react";

interface QuestionForm {
  exam_type: string;
  section: string;
  topic: string;
  prompt_text: string;
  difficulty: number;
  tags: string;
}

const INITIAL_FORM: QuestionForm = {
  exam_type: "TOEFL",
  section: "",
  topic: "",
  prompt_text: "",
  difficulty: 3,
  tags: "",
};

export default function NewQuestionPage() {
  const router = useRouter();
  const [form, setForm] = useState<QuestionForm>(INITIAL_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: name === "difficulty" ? Number(value) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const payload = {
        examType: form.exam_type,
        section: form.section,
        topic: form.topic,
        promptText: form.prompt_text,
        difficulty: form.difficulty,
        tags: form.tags
          .split(",")
          .map((t) => t.trim())
          .filter(Boolean),
      };
      await api.post("/questions", payload);
      router.push("/resources/questions");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "创建题目失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">新建题目</h1>

      <Card className="max-w-2xl">
        <form onSubmit={handleSubmit}>
          <CardHeader>
            <CardTitle>题目信息</CardTitle>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="rounded-lg border border-data-red/20 bg-data-red/5 p-3 text-sm text-data-red">
                {error}
              </div>
            )}

            {/* 考试类型 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">考试类型</label>
              <select
                name="exam_type"
                value={form.exam_type}
                onChange={handleChange}
                className="flex h-10 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="TOEFL">TOEFL</option>
                <option value="IELTS">IELTS</option>
              </select>
            </div>

            {/* 题型 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">题型 (Section)</label>
              <Input
                name="section"
                value={form.section}
                onChange={handleChange}
                placeholder="例如: Independent Speaking, Task 1"
                required
              />
            </div>

            {/* 话题 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">话题 (Topic)</label>
              <Input
                name="topic"
                value={form.topic}
                onChange={handleChange}
                placeholder="例如: Describe your favorite place"
              />
            </div>

            {/* 题目内容 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">题目内容</label>
              <textarea
                name="prompt_text"
                value={form.prompt_text}
                onChange={handleChange}
                placeholder="请输入完整的题目描述..."
                required
                rows={5}
                className="flex w-full rounded-lg border border-border bg-white px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>

            {/* 难度 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">难度</label>
              <select
                name="difficulty"
                value={form.difficulty}
                onChange={handleChange}
                className="flex h-10 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value={1}>1 - 入门</option>
                <option value={2}>2 - 基础</option>
                <option value={3}>3 - 中等</option>
                <option value={4}>4 - 进阶</option>
                <option value={5}>5 - 挑战</option>
              </select>
            </div>

            {/* 标签 */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-primary">标签</label>
              <Input
                name="tags"
                value={form.tags}
                onChange={handleChange}
                placeholder="用逗号分隔，例如: 日常, 校园, 社交"
              />
              <p className="text-xs text-muted-foreground">多个标签请用英文逗号分隔</p>
            </div>
          </CardContent>

          <CardFooter className="gap-3">
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              创建题目
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push("/resources/questions")}
            >
              取消
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
