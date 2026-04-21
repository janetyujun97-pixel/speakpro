"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, ArrowRight, Trash2 } from "lucide-react";
import { api } from "@/lib/api";
import FileUpload from "@/components/ui/file-upload";
import {
  Eyebrow,
  Mono,
  HairlineBtn,
  SectionRule,
} from "@/components/editorial/primitives";

interface QuestionData {
  id: string;
  examType: string;
  section: string;
  topic: string;
  promptText: string;
  difficulty: number;
  tags: string[];
  sampleAudioUrl: string | null;
}

export default function EditQuestionPage() {
  const params = useParams();
  const router = useRouter();
  const questionId = params.id as string;

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const [examType, setExamType] = useState("IELTS");
  const [section, setSection] = useState("");
  const [topic, setTopic] = useState("");
  const [promptText, setPromptText] = useState("");
  const [difficulty, setDifficulty] = useState(3);
  const [tags, setTags] = useState("");
  const [sampleAudioUrl, setSampleAudioUrl] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const data = await api.get<QuestionData>(`/questions/${questionId}`);
        setExamType(data.examType || "IELTS");
        setSection(data.section || "");
        setTopic(data.topic || "");
        setPromptText(data.promptText || "");
        setDifficulty(data.difficulty || 3);
        setTags((data.tags || []).join(", "));
        setSampleAudioUrl(data.sampleAudioUrl || "");
      } catch {
        setError("加载题目失败");
      } finally {
        setLoading(false);
      }
    })();
  }, [questionId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await api.put(`/questions/${questionId}`, {
        examType,
        section,
        topic,
        promptText,
        difficulty,
        tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
        sampleAudioUrl: sampleAudioUrl || null,
      });
      router.push("/resources?tab=questions");
    } catch {
      setError("保存失败，请重试");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 加载中 —</Mono>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <SectionRule
        className="mb-8"
        label={
          <span className="flex items-baseline gap-2">
            <span>题目信息</span>
            <Mono size={10}>· EDIT · {questionId.slice(0, 8).toUpperCase()}</Mono>
          </span>
        }
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
              value={examType}
              onChange={(e) => setExamType(e.target.value)}
              className="w-full border border-line bg-ivory px-3 py-2 text-[12px] text-ink outline-none"
            >
              <option value="IELTS">IELTS</option>
              <option value="TOEFL">TOEFL</option>
            </select>
          </Field>
          <Field label="难度">
            <select
              value={difficulty}
              onChange={(e) => setDifficulty(Number(e.target.value))}
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

        <Field label="题型 Section">
          <SerifInput value={section} onChange={setSection} placeholder="Part 2" required />
        </Field>

        <Field label="话题 Topic" hint="供题库筛选使用，可省略">
          <SerifInput
            value={topic}
            onChange={setTopic}
            placeholder="Describe a skill you would like to learn"
          />
        </Field>

        <Field label="题目内容">
          <textarea
            value={promptText}
            onChange={(e) => setPromptText(e.target.value)}
            rows={6}
            required
            className="w-full resize-y border border-line bg-ivory p-3 font-serif text-[14px] leading-[1.7] text-ink outline-none"
            style={{
              borderRadius: 2,
              minHeight: 140,
              fontVariationSettings: '"opsz" 144, "SOFT" 50',
            }}
          />
        </Field>

        <Field label="标签" hint="多个标签用英文逗号分隔">
          <SerifInput
            value={tags}
            onChange={setTags}
            placeholder="日常, 校园, 社交"
          />
        </Field>

        <Field label="音频样本" hint="可选，最大 20 MB">
          {sampleAudioUrl ? (
            <div className="flex items-center justify-between border border-line bg-ivory px-3 py-2.5">
              <Mono size={11} color="var(--ink)">
                {sampleAudioUrl}
              </Mono>
              <button
                type="button"
                onClick={() => setSampleAudioUrl("")}
                className="inline-flex items-center gap-1.5 px-2 py-1 text-[11px] transition-colors hover:text-accent"
                style={{ color: "var(--muted)" }}
              >
                <Trash2 className="h-[13px] w-[13px]" strokeWidth={1.3} />
                移除
              </button>
            </div>
          ) : (
            <FileUpload
              accept="audio/*,.mp3,.wav,.m4a"
              maxSizeMB={20}
              label="上传音频样本"
              onUploadComplete={(url: string) => setSampleAudioUrl(url)}
              onError={(err: string) => setError(err)}
            />
          )}
        </Field>

        <div className="flex items-center gap-3 border-t border-line pt-6">
          <HairlineBtn type="button" onClick={() => router.back()}>
            取消
          </HairlineBtn>
          <div className="flex-1" />
          <HairlineBtn
            primary
            type="submit"
            disabled={saving}
            rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
          >
            {saving ? "保存中…" : "保存修改"}
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
