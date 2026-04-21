"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Mono,
  Chip,
  HairlineBtn,
  SectionRule,
} from "@/components/editorial/primitives";

// ── Types ────────────────────────────────────────────────────────────

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

// ── Page ─────────────────────────────────────────────────────────────

export default function NewAssignmentPage() {
  const router = useRouter();

  // 业务状态
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [classId, setClassId] = useState("");
  const [selectedQuestionIds, setSelectedQuestionIds] = useState<string[]>([]);
  const [dueDate, setDueDate] = useState("");

  // 选项
  const [classes, setClasses] = useState<ClassOption[]>([]);
  const [questions, setQuestions] = useState<QuestionOption[]>([]);
  const [loadingOptions, setLoadingOptions] = useState(true);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const [classesData, questionsData] = await Promise.all([
          api.get<ClassOption[]>("/classes"),
          api.get<QuestionOption[]>("/questions"),
        ]);
        setClasses(classesData);
        setQuestions(questionsData);
      } catch (err) {
        setError(err instanceof Error ? err.message : "加载数据失败");
      } finally {
        setLoadingOptions(false);
      }
    })();
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
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建作业失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl">
      {/* 顶部标题条 */}
      <SectionRule
        label="作业信息 · NEW"
        right={
          <Link href="/assignments">
            <HairlineBtn
              leftIcon={<ArrowLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              返回列表
            </HairlineBtn>
          </Link>
        }
        className="mb-8"
      />

      {/* 错误条 */}
      {error && (
        <div
          className="mb-6 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      {loadingOptions ? (
        <div className="py-20 text-center">
          <Mono size={11}>— 加载数据中 —</Mono>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-7">
          {/* 标题 */}
          <Field label="标题">
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="如：Part 3 追问练习 × 3 题"
              required
              className="w-full border-0 border-b border-ink bg-transparent pb-1 font-serif text-[20px] text-ink outline-none placeholder:text-muted-2"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
          </Field>

          {/* 描述 */}
          <Field label="描述">
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="作业说明（可选）…"
              rows={3}
              className="w-full resize-y border border-line bg-ivory p-3 text-[12px] text-ink outline-none placeholder:text-muted-2"
              style={{ borderRadius: 2, minHeight: 72 }}
            />
          </Field>

          {/* 班级 + 截止日期 */}
          <div className="grid grid-cols-2 gap-8">
            <Field label="班级">
              <select
                value={classId}
                onChange={(e) => setClassId(e.target.value)}
                required
                className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
              >
                <option value="">请选择班级</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}（{c.examType}）
                  </option>
                ))}
              </select>
            </Field>

            <Field label="截止日期">
              <input
                type="datetime-local"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full border border-line bg-ivory px-3 py-2 font-mono text-[12px] text-ink outline-none"
              />
            </Field>
          </div>

          {/* 题目选择 */}
          <div>
            <div className="mb-3 flex items-baseline justify-between">
              <Eyebrow>选择题目</Eyebrow>
              <Mono size={10}>{selectedQuestionIds.length} / {questions.length} 已选</Mono>
            </div>
            {questions.length === 0 ? (
              <div className="border border-line bg-ivory py-10 text-center">
                <Mono size={11}>— 暂无题目，请先在题库中创建 —</Mono>
              </div>
            ) : (
              <div className="max-h-80 overflow-y-auto border border-line bg-ivory">
                {questions.map((q, i) => {
                  const selected = selectedQuestionIds.includes(q.id);
                  return (
                    <label
                      key={q.id}
                      className="flex cursor-pointer items-start gap-3 px-4 py-3 transition-colors hover:bg-bg-soft/60"
                      style={{
                        borderBottom:
                          i < questions.length - 1
                            ? "1px solid var(--line-soft)"
                            : 0,
                        background: selected ? "var(--bg-soft)" : "transparent",
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={selected}
                        onChange={() => toggleQuestion(q.id)}
                        className="mt-1 h-3.5 w-3.5 accent-ink"
                      />
                      <Serif
                        size={15}
                        italic
                        color={selected ? "var(--accent)" : "var(--muted-2)"}
                        style={{ width: 32, marginTop: 2 }}
                      >
                        {String(i + 1).padStart(2, "0")}
                      </Serif>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <Chip tone="muted">{q.examType}</Chip>
                          <Chip tone="muted">{q.section}</Chip>
                        </div>
                        <div className="mt-1.5 text-[13px] font-medium text-ink">
                          {q.topic || q.promptText}
                        </div>
                        {q.topic && q.promptText && (
                          <div className="mt-0.5 line-clamp-2 text-[11px] text-muted">
                            {q.promptText}
                          </div>
                        )}
                      </div>
                    </label>
                  );
                })}
              </div>
            )}
          </div>

          {/* 操作 */}
          <div className="flex items-center gap-3 border-t border-line pt-6">
            <HairlineBtn
              type="button"
              onClick={() => router.push("/assignments")}
            >
              取消
            </HairlineBtn>
            <div className="flex-1" />
            <HairlineBtn
              primary
              type="submit"
              disabled={submitting}
              rightIcon={
                <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
              }
            >
              {submitting ? "创建中…" : "创建作业"}
            </HairlineBtn>
          </div>
        </form>
      )}
    </div>
  );
}

// ── helpers ──────────────────────────────────────────────────────────

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Eyebrow>{label}</Eyebrow>
      <div>{children}</div>
    </div>
  );
}
