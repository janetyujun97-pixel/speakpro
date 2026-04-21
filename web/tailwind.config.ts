import type { Config } from "tailwindcss";

// SpeakPro Web 设计 token — editorial 风格：墨黑 + sienna + 暖米。
// source of truth = speakpro/components/HomeEditorial.jsx 的 PALETTE。
// 既有名字（primary/accent/success）保留，仅改值；新增 token 用新名字。

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // ——— 既有 token（改值） ———
        primary: "#1C1B18",      // ink，墨黑
        accent: "#B54A25",       // sienna
        success: "#2F4A3A",      // moss（替代薄荷）

        // ——— 新增 token ———
        ink: "#1C1B18",
        bg: "#F4EEE3",
        bgSoft: "#EDE5D6",
        ivory: "#FBF8F2",
        muted: "#706A5E",
        line: "rgba(28,27,24,0.12)",
        accentSoft: "rgba(181,74,37,0.08)",
        accentWarm: "#D9734A",
        moss: "#2F4A3A",
        mossSoft: "rgba(47,74,58,0.08)",

        data: {
          blue: "#3B82F6",
          green: "#10B981",
          orange: "#F59E0B",
          red: "#EF4444",
        },
      },
      fontFamily: {
        sans: [
          '"Inter Variable"',
          "Inter",
          "var(--font-geist-sans)",
          '"PingFang SC"',
          "-apple-system",
          "system-ui",
          "sans-serif",
        ],
        serif: [
          '"Fraunces Variable"',
          "Fraunces",
          '"Source Serif Pro"',
          "Georgia",
          "serif",
        ],
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
