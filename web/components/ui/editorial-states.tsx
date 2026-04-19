"use client";

import { useEffect, useState } from "react";
import { ArrowRight } from "lucide-react";
import { cn } from "@/lib/utils";

// =============================================================================
// Editorial EmptyState · 编辑式空态
// 大号斜体 `0` + 红色斜划线 + 两行 serif 标题（正体 + italic 副句）。
// 与既有 <EmptyState> 并存；新页面优先用此版本贴合 editorial 设计语言。
// =============================================================================

interface EditorialEmptyStateProps {
  eyebrow: string;
  headline: string;
  headlineItalic: string;
  message: string;
  primaryCTA?: { title: string; onClick: () => void };
  secondaryCTA?: { title: string; onClick: () => void };
  footer?: string;
  footerNumber?: string;
  className?: string;
}

export function EditorialEmptyState({
  eyebrow,
  headline,
  headlineItalic,
  message,
  primaryCTA,
  secondaryCTA,
  footer = "EMPTY STATE",
  footerNumber = "N° 001",
  className,
}: EditorialEmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center bg-[var(--background)] min-h-[540px] px-9",
        className,
      )}
    >
      <div className="flex-1" />

      {/* Zero glyph */}
      <div className="relative mb-7 h-[140px] w-[120px] flex items-center justify-center">
        <span
          className="font-serif italic text-[140px] leading-none"
          style={{ color: "var(--border)", fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          0
        </span>
        <span
          className="absolute block h-px w-[60px] bg-accent"
          style={{ transform: "rotate(-15deg)" }}
          aria-hidden
        />
      </div>

      <div className="eyebrow" style={{ color: "var(--ring)" }}>
        {eyebrow}
      </div>

      <div className="mt-3.5 text-center">
        <div
          className="text-[28px] leading-tight"
          style={{ fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          {headline}
        </div>
        <div
          className="text-[28px] leading-tight italic"
          style={{ fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          {headlineItalic}
        </div>
      </div>

      <p className="mt-4 text-sm text-muted-foreground max-w-[240px] text-center leading-relaxed whitespace-pre-line">
        {message}
      </p>

      {primaryCTA && (
        <button
          onClick={primaryCTA.onClick}
          className="mt-8 inline-flex items-center gap-2 rounded-full bg-[var(--foreground)] px-7 py-3.5 text-[13px] font-semibold text-white hover:opacity-90 transition"
        >
          {primaryCTA.title}
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      )}
      {secondaryCTA && (
        <button
          onClick={secondaryCTA.onClick}
          className="mt-3.5 text-[11px] font-semibold text-accent hover:underline"
        >
          {secondaryCTA.title}
        </button>
      )}

      <div className="flex-1" />

      {/* Decorative footer rule */}
      <div className="w-full pb-6">
        <div className="flex justify-between text-[9px] font-semibold tracking-[0.2em] text-muted-foreground pb-2">
          <span>{footer}</span>
          <span>{footerNumber}</span>
        </div>
        <div className="h-px bg-[var(--border)]" />
      </div>
    </div>
  );
}

// =============================================================================
// Editorial Skeleton · 卡片 + Fraunces italic "patience" loader
// =============================================================================

interface EditorialSkeletonProps {
  headerTitle?: string;
  cardCount?: number;
  className?: string;
}

export function EditorialSkeleton({
  headerTitle = "LOADING · 加载中",
  cardCount = 3,
  className,
}: EditorialSkeletonProps) {
  const [pulse, setPulse] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setPulse((p) => p + 1), 1000);
    return () => clearInterval(t);
  }, []);
  const dotsFrames = ["●○○", "○●○", "○○●"];

  return (
    <div
      className={cn(
        "flex flex-col bg-[var(--background)] min-h-[540px]",
        className,
      )}
    >
      {/* masthead */}
      <div className="px-6 py-4 border-b" style={{ borderColor: "var(--border)" }}>
        <span className="eyebrow text-muted-foreground">{headerTitle}</span>
      </div>

      {/* hero placeholder */}
      <div className="px-6 pt-4 opacity-60 space-y-1.5">
        <div className="h-7 w-[220px] rounded bg-[var(--border)]" />
        <div className="h-7 w-[170px] rounded bg-[var(--border)]" />
      </div>

      {/* cards */}
      <div className="px-6 pt-7 space-y-2.5">
        {Array.from({ length: cardCount }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-3 rounded-lg border p-4"
            style={{
              borderColor: "var(--border)",
              background: "var(--muted)",
              opacity: 1 - i * 0.15,
            }}
          >
            <div className="h-11 w-11 rounded-lg bg-[var(--border)]" />
            <div className="flex-1 space-y-1.5">
              <div className="h-2.5 w-20 rounded bg-[var(--border)]" />
              <div className="h-3.5 w-4/5 rounded bg-[var(--border)]" />
              <div className="h-2 w-1/2 rounded bg-[var(--border)]" />
            </div>
            <div className="h-5 w-8 rounded bg-[var(--border)]" />
          </div>
        ))}
      </div>

      <div className="flex-1" />

      {/* typographic loader */}
      <div className="pb-9 text-center space-y-1">
        <div
          className="text-[32px] italic text-muted-foreground leading-none"
          style={{ fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          patience
        </div>
        <div className="eyebrow text-muted-foreground">
          FETCHING · {dotsFrames[pulse % 3]}
        </div>
      </div>
    </div>
  );
}

// =============================================================================
// Editorial ErrorState · 大号数字错误码 + 红色斜划线
// 错误码 + 文案映射（§4.3）。Web 端只暴露用得到的子集。
// =============================================================================

export type SpErrorCode =
  | "ERR-SCORE-503"
  | "ERR-TTS-504"
  | "ERR-NET"
  | "ERR-AUTH-401"
  | "ERR-UNKNOWN";

const ERROR_MAP: Record<SpErrorCode, {
  number: string;
  eyebrow: string;
  headline: string;
  headlineItalic: string;
  body: string;
}> = {
  "ERR-SCORE-503": {
    number: "503",
    eyebrow: "SCORE ENGINE UNREACHABLE",
    headline: "Our scoring engine",
    headlineItalic: "is catching its breath.",
    body: "评测引擎暂时无响应。已提交的批改和内容均已保存，稍后自动重试。",
  },
  "ERR-TTS-504": {
    number: "504",
    eyebrow: "VOICE SYNTH SLOW",
    headline: "The voice synthesis",
    headlineItalic: "is catching up.",
    body: "示范发音生成较慢，已切换到备用通道。你可以继续操作。",
  },
  "ERR-NET": {
    number: "—",
    eyebrow: "NO CONNECTION · 无网络",
    headline: "We're offline,",
    headlineItalic: "please retry.",
    body: "检查下网络连接后再试一次。",
  },
  "ERR-AUTH-401": {
    number: "401",
    eyebrow: "SESSION EXPIRED · 会话过期",
    headline: "Please log in again,",
    headlineItalic: "for continuity.",
    body: "为保护账号安全，请重新登录一次。",
  },
  "ERR-UNKNOWN": {
    number: "?",
    eyebrow: "SOMETHING WRONG · 出错了",
    headline: "Something odd,",
    headlineItalic: "please retry.",
    body: "发生了预期之外的问题。请重试，或反馈给我们。",
  },
};

/** 从 Error / status code 推断 SpErrorCode */
export function mapErrorToCode(err: unknown): SpErrorCode {
  const message = err instanceof Error ? err.message : String(err);
  if (/503/.test(message)) return "ERR-SCORE-503";
  if (/504/.test(message)) return "ERR-TTS-504";
  if (/401/.test(message)) return "ERR-AUTH-401";
  if (/network|fetch|offline/i.test(message)) return "ERR-NET";
  return "ERR-UNKNOWN";
}

interface EditorialErrorStateProps {
  code: SpErrorCode;
  onRetry?: () => void;
  onFeedback?: () => void;
  className?: string;
}

export function EditorialErrorState({
  code,
  onRetry,
  onFeedback,
  className,
}: EditorialErrorStateProps) {
  const spec = ERROR_MAP[code];

  return (
    <div
      className={cn(
        "flex flex-col items-center bg-[var(--background)] min-h-[540px] px-8",
        className,
      )}
    >
      <div className="flex-1" />

      {/* Number glyph + red slash */}
      <div className="relative flex items-center justify-center py-4">
        <span
          className="text-[110px] leading-none italic"
          style={{
            color: "var(--foreground)",
            fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif",
          }}
        >
          {spec.number}
        </span>
        <span
          className="absolute block h-[2px] w-[85%] bg-accent"
          style={{ transform: "rotate(-8deg)" }}
          aria-hidden
        />
      </div>

      <div className="eyebrow mt-3" style={{ color: "var(--ring)" }}>
        {spec.eyebrow}
      </div>

      <div className="mt-5 text-center">
        <div
          className="text-[26px] leading-tight"
          style={{ fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          {spec.headline}
        </div>
        <div
          className="text-[26px] leading-tight italic text-accent"
          style={{ fontFamily: "'Fraunces Variable', Fraunces, Georgia, serif" }}
        >
          {spec.headlineItalic}
        </div>
      </div>

      <p className="mt-4 text-sm text-muted-foreground max-w-[260px] text-center leading-relaxed">
        {spec.body}
      </p>

      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-7 inline-flex items-center gap-2 rounded-full bg-[var(--foreground)] px-7 py-3.5 text-[13px] font-semibold text-white hover:opacity-90 transition"
        >
          再试一次
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      )}

      <div className="mt-3 flex items-center gap-1 text-[11px]">
        {onFeedback && (
          <>
            <button
              onClick={onFeedback}
              className="font-semibold text-accent hover:underline"
            >
              反馈问题
            </button>
            <span className="text-[var(--border)]">·</span>
          </>
        )}
        <span className="text-muted-foreground">错误码 {code}</span>
      </div>

      <div className="flex-1" />

      <div className="w-full pb-6">
        <div className="flex justify-between text-[9px] font-semibold tracking-[0.2em] text-muted-foreground pb-2">
          <span>ERROR STATE</span>
          <span>N° {spec.number}</span>
        </div>
        <div className="h-px bg-[var(--border)]" />
      </div>
    </div>
  );
}
