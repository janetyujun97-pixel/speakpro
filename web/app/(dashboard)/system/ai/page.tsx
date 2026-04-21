"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { RefreshCw } from "lucide-react";
import { api } from "@/lib/api";
import { getUser } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";

// 系统默认值来源: Go /api/v1/health，经 NestJS /api/v1/system/ai-health 代理
interface AiHealth {
  status: "ok" | "down" | string;
  ai: {
    asr?: string;
    ise?: string;
    llm?: string;
    tts?: string;
  } | null;
  error?: string;
}

const PROVIDER_LABELS: Record<string, string> = {
  tencent: "腾讯云",
  xunfei: "讯飞",
  mimo: "MiMo",
  qwen: "通义千问",
  fish: "Fish Audio",
};

const GROUPS: Array<{
  key: keyof NonNullable<AiHealth["ai"]>;
  title: string;
  en: string;
  desc: string;
  options: string[];
}> = [
  { key: "asr", title: "ASR 语音识别", en: "Speech Recognition", desc: "实时 / 离线语音转写",     options: ["tencent", "xunfei"] },
  { key: "ise", title: "ISE 发音评测", en: "Pronunciation",      desc: "跟读练习评分引擎",         options: ["tencent", "xunfei"] },
  { key: "llm", title: "LLM 大模型",   en: "Language Model",     desc: "对话与写作评析",           options: ["mimo", "qwen"] },
  { key: "tts", title: "TTS 语音合成", en: "Text-to-Speech",     desc: "AI 播报与对话回复",       options: ["mimo", "fish", "xunfei"] },
];

export default function AiServicePage() {
  const router = useRouter();
  const [health, setHealth] = useState<AiHealth | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await api.get<AiHealth>("/system/ai-health");
      setHealth(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载失败");
      setHealth(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const user = getUser();
    if (!user || user.role !== "admin") {
      router.replace("/");
      return;
    }
    load();
  }, [router, load]);

  const ok = health?.status === "ok";
  const active = (health?.ai || {}) as Record<string, string | undefined>;
  const errorText = error || (!ok && health?.error) || "";

  return (
    <div className="mx-auto max-w-4xl">
      {/* 顶栏 */}
      <div className="mb-6 flex items-start justify-between border-b border-line pb-4">
        <div>
          <Eyebrow>系统运行时 · RUNTIME</Eyebrow>
          <div className="mt-1.5">
            <Serif size={26}>AI 服务编排</Serif>
          </div>
          <div className="mt-1">
            <Mono size={10}>Go orchestrator · 当前装载的提供商</Mono>
          </div>
        </div>
        <HairlineBtn
          onClick={load}
          disabled={loading}
          leftIcon={
            <RefreshCw
              className={`h-[13px] w-[13px] ${loading ? "animate-spin" : ""}`}
              strokeWidth={1.3}
            />
          }
        >
          刷新
        </HairlineBtn>
      </div>

      {/* 健康横幅 */}
      <div className="mb-7 flex items-center gap-3 border border-line bg-ivory px-5 py-4">
        <span
          className="inline-block h-2.5 w-2.5"
          style={{
            borderRadius: 999,
            background: loading
              ? "var(--muted-2)"
              : ok
                ? "var(--moss)"
                : "var(--accent)",
          }}
        />
        <div className="flex-1">
          <Serif size={16}>
            {loading ? "检测中…" : ok ? "Go 服务运行正常" : "Go 服务不可达"}
          </Serif>
          {errorText && (
            <div className="mt-0.5">
              <Mono size={10} color="var(--accent)">{errorText}</Mono>
            </div>
          )}
        </div>
      </div>

      {/* 提供商网格 */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {GROUPS.map((g, i) => {
          const activeValue = active[g.key];
          return (
            <div
              key={g.key}
              className="border border-line bg-ivory p-5"
              style={{
                borderLeft: i === 0 ? "3px solid var(--accent)" : "1px solid var(--line)",
              }}
            >
              <div className="flex items-baseline gap-2">
                <Serif size={18}>{g.title}</Serif>
                <Serif size={12} italic color="var(--muted)">
                  {g.en}
                </Serif>
              </div>
              <div className="mt-0.5">
                <Mono size={10}>{g.desc}</Mono>
              </div>

              <div className="mt-4">
                <Eyebrow>当前默认</Eyebrow>
                <div className="mt-1.5 flex items-center gap-2">
                  {activeValue ? (
                    <>
                      <span
                        className="inline-block h-2 w-2"
                        style={{
                          borderRadius: 999,
                          background: ok ? "var(--moss)" : "var(--muted-2)",
                        }}
                      />
                      <Serif size={18}>{PROVIDER_LABELS[activeValue] || activeValue}</Serif>
                    </>
                  ) : (
                    <Mono size={11}>— 未获取 —</Mono>
                  )}
                </div>
              </div>

              <div className="mt-4 border-t border-line-soft pt-3">
                <Eyebrow>可用备选</Eyebrow>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {g.options.map((opt) => (
                    <Chip
                      key={opt}
                      tone={opt === activeValue ? "ink" : "muted"}
                    >
                      {PROVIDER_LABELS[opt] || opt}
                    </Chip>
                  ))}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* 说明（编辑体） */}
      <div className="mt-8 border-l-2 border-gold bg-ivory px-5 py-4">
        <Eyebrow color="var(--gold)">切换系统默认提供商</Eyebrow>
        <p className="mt-2 text-[12px] leading-relaxed text-muted">
          当前版本不支持在后台热切换系统默认值。如需调整，请修改服务器{" "}
          <code
            className="px-1"
            style={{ background: "var(--bg-soft)", fontFamily: "var(--font-mono)" }}
          >
            server/.env
          </code>{" "}
          的{" "}
          <code
            className="px-1"
            style={{ background: "var(--bg-soft)", fontFamily: "var(--font-mono)" }}
          >
            DEFAULT_ASR_PROVIDER
          </code>{" "}
          /{" "}
          <code
            className="px-1"
            style={{ background: "var(--bg-soft)", fontFamily: "var(--font-mono)" }}
          >
            DEFAULT_ISE_PROVIDER
          </code>{" "}
          /{" "}
          <code
            className="px-1"
            style={{ background: "var(--bg-soft)", fontFamily: "var(--font-mono)" }}
          >
            DEFAULT_LLM_PROVIDER
          </code>{" "}
          /{" "}
          <code
            className="px-1"
            style={{ background: "var(--bg-soft)", fontFamily: "var(--font-mono)" }}
          >
            DEFAULT_TTS_PROVIDER
          </code>{" "}
          后重启 Go 服务。用户级偏好仍可在{" "}
          <span className="text-ink">
            <em>设置</em>
          </span>{" "}
          页独立配置。
        </p>
      </div>
    </div>
  );
}
