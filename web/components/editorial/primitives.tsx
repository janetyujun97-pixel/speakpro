// Editorial primitives — 对齐 design/SpeakPro Admin/primitives.jsx
// 所有组件都走 CSS 变量（见 globals.css），主题切换零改动
import { ReactNode, CSSProperties, ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

// ── Typography ─────────────────────────────────────────────────────────

export function Eyebrow({
  children,
  color,
  className,
  style,
}: {
  children: ReactNode;
  color?: string;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <span
      className={cn("font-sans font-semibold uppercase", className)}
      style={{
        fontSize: 10,
        letterSpacing: "0.22em",
        color: color || "var(--muted)",
        ...style,
      }}
    >
      {children}
    </span>
  );
}

export function Serif({
  children,
  size = 24,
  italic = false,
  color,
  weight = 400,
  className,
  style,
  as: As = "span",
}: {
  children: ReactNode;
  size?: number;
  italic?: boolean;
  color?: string;
  weight?: 400 | 500 | 600;
  className?: string;
  style?: CSSProperties;
  as?: "span" | "h1" | "h2" | "h3" | "h4" | "div";
}) {
  return (
    <As
      className={cn("font-serif", className)}
      style={{
        fontWeight: weight,
        fontSize: size,
        lineHeight: 1.1,
        letterSpacing: -0.5,
        fontStyle: italic ? "italic" : "normal",
        color: color || "var(--ink)",
        fontVariationSettings: '"opsz" 144, "SOFT" 50',
        fontFeatureSettings: '"ss01"',
        ...style,
      }}
    >
      {children}
    </As>
  );
}

export function Numeral({
  children,
  size = 42,
  color,
  className,
  style,
}: {
  children: ReactNode;
  size?: number;
  color?: string;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <span
      className={cn("font-serif", className)}
      style={{
        fontWeight: 400,
        fontSize: size,
        fontVariationSettings: '"opsz" 144, "SOFT" 50',
        color: color || "var(--ink)",
        letterSpacing: -1,
        lineHeight: 0.9,
        fontFeatureSettings: '"ss01"',
        ...style,
      }}
    >
      {children}
    </span>
  );
}

export function Mono({
  children,
  size = 11,
  color,
  className,
  style,
}: {
  children: ReactNode;
  size?: number;
  color?: string;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <span
      className={cn("font-mono", className)}
      style={{
        fontSize: size,
        letterSpacing: 0.5,
        color: color || "var(--muted)",
        ...style,
      }}
    >
      {children}
    </span>
  );
}

// ── Chips & Buttons ────────────────────────────────────────────────────

export type ChipTone = "default" | "accent" | "moss" | "ink" | "muted" | "warn";

const CHIP_TONES: Record<ChipTone, { bg: string; bd: string; fg: string }> = {
  default: { bg: "transparent", bd: "var(--line)", fg: "var(--ink)" },
  accent:  { bg: "var(--accent-soft)", bd: "var(--accent)", fg: "var(--accent)" },
  moss:    { bg: "var(--moss-soft)", bd: "var(--moss)", fg: "var(--moss)" },
  ink:     { bg: "var(--ink)", bd: "var(--ink)", fg: "var(--ivory)" },
  muted:   { bg: "transparent", bd: "var(--line)", fg: "var(--muted)" },
  warn:    { bg: "rgba(154,122,31,0.09)", bd: "#9A7A1F", fg: "#9A7A1F" },
};

export function Chip({
  children,
  tone = "default",
  className,
  style,
}: {
  children: ReactNode;
  tone?: ChipTone;
  className?: string;
  style?: CSSProperties;
}) {
  const t = CHIP_TONES[tone];
  return (
    <span
      className={cn("inline-block font-sans font-semibold uppercase", className)}
      style={{
        padding: "2px 8px",
        border: `1px solid ${t.bd}`,
        background: t.bg,
        color: t.fg,
        fontSize: 10,
        letterSpacing: 1.5,
        borderRadius: 2,
        ...style,
      }}
    >
      {children}
    </span>
  );
}

interface HairlineBtnProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "style"> {
  primary?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  style?: CSSProperties;
}

export function HairlineBtn({
  children,
  primary,
  leftIcon,
  rightIcon,
  className,
  style,
  ...rest
}: HairlineBtnProps) {
  return (
    <button
      {...rest}
      className={cn("inline-flex items-center gap-2 font-sans", className)}
      style={{
        padding: "9px 16px",
        border: primary ? 0 : "1px solid var(--line)",
        cursor: "pointer",
        background: primary ? "var(--ink)" : "transparent",
        color: primary ? "var(--ivory)" : "var(--ink)",
        fontSize: 12,
        fontWeight: 600,
        letterSpacing: 0.3,
        borderRadius: 2,
        ...style,
      }}
    >
      {leftIcon}
      {children}
      {rightIcon}
    </button>
  );
}

// ── Structure ──────────────────────────────────────────────────────────

export function SectionRule({
  label,
  right,
  className,
  style,
}: {
  label: ReactNode;
  right?: ReactNode;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <div
      className={cn("flex items-center justify-between border-b border-line pb-3", className)}
      style={style}
    >
      <Eyebrow>{label}</Eyebrow>
      {right}
    </div>
  );
}

export function Divider({
  className,
  style,
}: {
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <div
      className={cn("h-px bg-line", className)}
      style={style}
    />
  );
}
