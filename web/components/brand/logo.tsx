// SpeakPro 品牌 Logo — 对齐 design/SpeakPro Icons/ 的 Section 03
//
// 变体：
//   horizontal  —— 44px seal + Speak/Pro wordmark + 副标（页面 Header 用）
//   stacked     —— 大 Edition №XXX 竖排（侧栏展开态 masthead）
//   avatar      —— 单个 44px 赭石 seal，带小麦克风（侧栏折叠/聊天头像）
//   favicon     —— 32px 简化版，只保留赭石底 + S
//
// 可选：`inverted` —— 深色底反白（avatar/favicon 适用）

import type { CSSProperties, ReactNode } from "react";

export type LogoVariant = "horizontal" | "stacked" | "avatar" | "favicon";

interface LogoProps {
  variant?: LogoVariant;
  /** 默认 sienna 底；设 true 则 ivory 底、ink S、accent 麦（深色界面用） */
  inverted?: boolean;
  /** 覆盖 seal 像素高度（默认随 variant） */
  size?: number;
  /** 覆盖右侧副标内容（仅 horizontal） */
  subtitle?: ReactNode;
  /** 覆盖右侧副标 edition 号（仅 stacked） */
  edition?: string;
  /** stacked 日期行（weekday · Month D, YYYY） */
  dateLine?: string;
  className?: string;
  style?: CSSProperties;
}

export function Logo(props: LogoProps) {
  const { variant = "horizontal" } = props;
  switch (variant) {
    case "horizontal": return <HorizontalLogo {...props} />;
    case "stacked":    return <StackedLogo {...props} />;
    case "avatar":     return <AvatarLogo {...props} />;
    case "favicon":    return <FaviconLogo {...props} />;
  }
}

// ── Seal SVG（共用核心） ─────────────────────────────────────────────
function Seal({
  size = 44,
  inverted,
  withMic = true,
}: {
  size?: number;
  inverted?: boolean;
  withMic?: boolean;
}) {
  const bg = inverted ? "#FBF8F2" : "#B54A25";
  const fg = inverted ? "#1C1B18" : "#FBF8F2";
  const mic = inverted ? "#B54A25" : "#FBF8F2";
  return (
    <svg
      viewBox="0 0 64 64"
      width={size}
      height={size}
      aria-hidden="true"
      style={{ flexShrink: 0 }}
    >
      <rect x="0" y="0" width="64" height="64" rx="14" fill={bg} />
      <text
        x="32"
        y="46"
        textAnchor="middle"
        fill={fg}
        fontFamily="'Fraunces', Georgia, serif"
        fontSize="52"
        fontStyle="italic"
        fontWeight="500"
        style={{ fontVariationSettings: '"opsz" 144' } as CSSProperties}
      >
        S
      </text>
      {withMic && (
        <g transform="translate(44, 22)" fill={mic} stroke="none">
          <rect x="-4" y="-7" width="8" height="12" rx="4" />
        </g>
      )}
    </svg>
  );
}

// ── A · Horizontal ───────────────────────────────────────────────────
function HorizontalLogo({
  size = 44,
  inverted,
  subtitle = "TEACHER CONSOLE · EST 2026",
  className,
  style,
}: LogoProps) {
  return (
    <div
      className={className}
      style={{ display: "flex", alignItems: "center", gap: 16, ...style }}
    >
      <Seal size={size} inverted={inverted} />
      <div>
        <div
          style={{
            fontFamily: "var(--font-serif), 'Fraunces', Georgia, serif",
            fontSize: Math.round(size * 0.68),
            fontWeight: 500,
            letterSpacing: -0.5,
            lineHeight: 1,
            color: inverted ? "var(--ivory)" : "var(--ink)",
            fontVariationSettings: '"opsz" 144',
          } as CSSProperties}
        >
          Speak
          <span
            style={{
              fontStyle: "italic",
              fontWeight: 400,
              color: "var(--accent)",
            }}
          >
            Pro
          </span>
        </div>
        {subtitle && (
          <div
            style={{
              marginTop: 4,
              fontFamily: "var(--font-mono), 'JetBrains Mono', monospace",
              fontSize: 10,
              letterSpacing: 1.5,
              color: "var(--muted)",
            }}
          >
            {subtitle}
          </div>
        )}
      </div>
    </div>
  );
}

// ── B · Stacked（侧栏 masthead） ─────────────────────────────────────
function StackedLogo({
  edition = "№042",
  dateLine = "",
  className,
  style,
}: LogoProps) {
  return (
    <div className={className} style={style}>
      <div
        style={{
          fontFamily: "var(--font-mono), 'JetBrains Mono', monospace",
          fontSize: 10,
          letterSpacing: 2.5,
          color: "var(--muted)",
          textTransform: "uppercase",
        }}
      >
        SpeakPro · TEACHER
      </div>
      <div
        style={{
          marginTop: 8,
          display: "flex",
          alignItems: "baseline",
          gap: 8,
        }}
      >
        <span
          style={{
            fontFamily: "var(--font-serif), 'Fraunces', Georgia, serif",
            fontSize: 26,
            fontWeight: 500,
            letterSpacing: -0.5,
            lineHeight: 1,
            color: "var(--ink)",
            fontVariationSettings: '"opsz" 144',
          } as CSSProperties}
        >
          Edition
        </span>
        <span
          style={{
            fontFamily: "var(--font-serif), 'Fraunces', Georgia, serif",
            fontSize: 26,
            fontWeight: 400,
            fontStyle: "italic",
            color: "var(--accent)",
            letterSpacing: -0.5,
            lineHeight: 1,
            fontVariationSettings: '"opsz" 144',
          } as CSSProperties}
        >
          {edition}
        </span>
      </div>
      {dateLine && (
        <div
          style={{
            marginTop: 8,
            paddingTop: 8,
            borderTop: "1px solid var(--line)",
            fontFamily: "var(--font-mono), 'JetBrains Mono', monospace",
            fontSize: 10,
            letterSpacing: 1.2,
            color: "var(--muted)",
            textTransform: "uppercase",
          }}
        >
          {dateLine}
        </div>
      )}
    </div>
  );
}

// ── C · Avatar ───────────────────────────────────────────────────────
function AvatarLogo({ size = 44, inverted, className, style }: LogoProps) {
  return (
    <span className={className} style={{ display: "inline-flex", ...style }}>
      <Seal size={size} inverted={inverted} withMic />
    </span>
  );
}

// ── D · Favicon（简化，只 S） ───────────────────────────────────────
function FaviconLogo({ size = 32, inverted, className, style }: LogoProps) {
  return (
    <span className={className} style={{ display: "inline-flex", ...style }}>
      <Seal size={size} inverted={inverted} withMic={false} />
    </span>
  );
}
