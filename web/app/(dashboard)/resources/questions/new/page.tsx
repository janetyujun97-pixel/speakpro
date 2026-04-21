"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Mono,
  HairlineBtn,
  SectionRule,
} from "@/components/editorial/primitives";

interface QuestionForm {
  examType: string;
  section: string;
  topic: string;
  promptText: string;
  difficulty: number;
  tags: string;
}

const INITIAL: QuestionForm = {
  examType: "IELTS",
  section: "",
  topic: "",
  promptText: "",
  difficulty: 3,
  tags: "",
};

export default function NewQuestionPage() {
  const router = useRouter();
  const [form, setForm] = useState<QuestionForm>(INITIAL);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const patch = <K extends keyof QuestionForm>(k: K, v: QuestionForm[K]) =>
    setForm((prev) => ({ ...prev, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await api.post("/questions", {
        examType: form.examType,
        section: form.section,
        topic: form.topic,
        promptText: form.promptText,
        difficulty: form.difficulty,
        tags: form.tags.split(",").map((t) => t.trim()).filter(Boolean),
      });
      router.push("/resources?tab=questions");
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建题目失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl">
      <SectionRule
        className="mb-8"
        label="题目信息 · NEW"
        right={
          <Link href="/resources?tab=questions">
            <HairlineBtn
              leftIcon={<ArrowLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              返回题库
            </HairlineBtn>
          </Link>
        }
      />

      {error && (
        <div
          className="mb-6 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]"
          style={{ color: "var(--accent)" }}
        >
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-2 gap-6">
          <Field label="考试类型">
            <select
              value={form.examType}
              onChange={(e) => patch("examType", e.target.value)}
              className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
            >
              <option value="IELTS">IELTS</option>
              <option value="TOEFL">TOEFL</option>
            </select>
          </Field>
          <Field label="难度">
            <select
              value={form.difficulty}
              onChange={(e) => patch("difficulty", Number(e.target.value))}
              className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
            >
              {[1, 2, 3, 4, 5].map((d) => (
                <option key={d} value={d}>
                  {d} — {["入门", "基础", "中等", "进阶", "挑战"][d - 1]}
                </option>
              ))}
            </select>
          </Field>
        </div>

        <Field label="题型 Section" hint="例如：Part 1 / Part 2 / Part 3 / 朗读 / 跟读 / 模考">
          <SerifInput
            value={form.section}
            onChange={(v) => patch("section", v)}
            placeholder="Part 2"
            required
          />
        </Field>

        <Field label="话题 Topic" hint="供题库筛选使用，可省略">
          <SerifInput
            value={form.topic}
            onChange={(v) => patch("topic", v)}
            placeholder="Describe a skill you would like to learn"
          />
        </Field>

        <Field label="题目内容">
          <textarea
            value={form.promptText}
            onChange={(e) => patch("promptText", e.target.value)}
            rows={6}
            required
            placeholder="请输入完整的题目描述……"
            className="w-full resize-y border border-line bg-ivory p-3 font-serif text-[14px] leading-[1.7] text-ink outline-none placeholder:text-muted-2"
            style={{
              borderRadius: 2,
              minHeight: 140,
              fontVariationSettings: '"opsz" 144, "SOFT" 50',
            }}
          />
        </Field>

        <Field label="标签" hint="多个标签用英文逗号分隔">
          <SerifInput
            value={form.tags}
            onChange={(v) => patch("tags", v)}
            placeholder="日常, 校园, 社交"
          />
        </Field>

        <div className="flex items-center gap-3 border-t border-line pt-6">
          <HairlineBtn
            type="button"
            onClick={() => router.push("/resources?tab=questions")}
          >
            取消
          </HairlineBtn>
          <div className="flex-1" />
          <HairlineBtn
            primary
            type="submit"
            disabled={submitting}
            rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
          >
            {submitting ? "创建中…" : "创建题目"}
          </HairlineBtn>
        </div>
      </form>
    </div>
  );
}

// ── helpers ─────────────────────────────────────────────────────────

function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <Eyebrow>{label}</Eyebrow>
      <div className="mt-2">{children}</div>
      {hint && (
        <div className="mt-1.5">
          <Mono size={10}>{hint}</Mono>
        </div>
      )}
    </div>
  );
}

function SerifInput({
  value,
  onChange,
  placeholder,
  required,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      required={required}
      className="w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
      style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
    />
  );
}
