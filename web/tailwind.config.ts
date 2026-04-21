import type { Config } from "tailwindcss";

// Editorial design tokens 映射自 design/SpeakPro Admin/index.html
// 颜色均由 CSS 变量驱动，主题切换时只改 CSS 变量即可（见 globals.css）
const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: "var(--bg)",
        "bg-soft": "var(--bg-soft)",
        ivory: "var(--ivory)",
        ink: "var(--ink)",
        "ink-2": "var(--ink-2)",
        muted: "var(--muted)",
        "muted-2": "var(--muted-2)",
        line: "var(--line)",
        "line-soft": "var(--line-soft)",
        accent: "var(--accent)",
        "accent-soft": "var(--accent-soft)",
        moss: "var(--moss)",
        "moss-soft": "var(--moss-soft)",
        gold: "var(--gold)",
      },
      fontFamily: {
        sans: ['var(--font-inter)', '"PingFang SC"', '-apple-system', 'system-ui', 'sans-serif'],
        serif: ['var(--font-fraunces)', '"Source Serif Pro"', 'Georgia', 'serif'],
        mono: ['var(--font-mono)', '"SF Mono"', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        eyebrow: ["10px", { letterSpacing: "0.18em", lineHeight: "1.4" }],
      },
      letterSpacing: {
        eyebrow: "0.18em",
      },
      borderRadius: {
        // editorial 风格：整站硬朗直角或 2px 圆角
        xs: "2px",
      },
      animation: {
        shimmer: "shimmer 2s infinite linear",
        fadeIn: "fadeIn 0.3s ease-out",
        slideUp: "slideUp 0.3s ease-out",
      },
      keyframes: {
        shimmer: {
          "0%": { backgroundPosition: "200% 0" },
          "100%": { backgroundPosition: "-200% 0" },
        },
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { opacity: "0", transform: "translateY(10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
    },
  },
  plugins: [],
};

export default config;
