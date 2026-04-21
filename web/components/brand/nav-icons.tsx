// SpeakPro 侧栏 6 大类导航图标 — 对齐 design/SpeakPro Icons/ Section 04
// 规范：24×24 viewBox · stroke-width 1.3 · round cap/join · fill=none · stroke=currentColor
// 用法：<NavIcon name="dashboard" size={17} />  颜色从父层 CSS color 继承

import type { SVGProps } from "react";

export type NavIconName =
  | "dashboard"
  | "classes"
  | "assignments"
  | "grading"
  | "library"
  | "analytics";

interface NavIconProps extends Omit<SVGProps<SVGSVGElement>, "name"> {
  name: NavIconName;
  size?: number;
  strokeWidth?: number;
}

export function NavIcon({
  name,
  size = 17,
  strokeWidth = 1.3,
  ...rest
}: NavIconProps) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": "true" as const,
    ...rest,
  };
  switch (name) {
    case "dashboard":
      // 01 · 四象限网格
      return (
        <svg {...common}>
          <path d="M3 13h8V3H3v10zM13 21h8V11h-8v10zM3 21h8v-6H3v6zM13 3v6h8V3h-8z" />
        </svg>
      );
    case "classes":
      // 02 · 双人组
      return (
        <svg {...common}>
          <circle cx="9" cy="8" r="3.5" />
          <path d="M3 21a6 6 0 0 1 12 0" />
          <circle cx="17" cy="6" r="2.5" />
          <path d="M15 13a5 5 0 0 1 6 5" />
        </svg>
      );
    case "assignments":
      // 03 · 记事夹
      return (
        <svg {...common}>
          <rect x="6" y="4" width="12" height="17" rx="1.5" />
          <rect x="9" y="2" width="6" height="4" rx="1" />
          <path d="M9 11h6M9 15h4" />
        </svg>
      );
    case "grading":
      // 04 · 麦克风 + 拱弧
      return (
        <svg {...common}>
          <rect x="9" y="3" width="6" height="12" rx="3" />
          <path d="M5 11a7 7 0 0 0 14 0" />
          <path d="M12 18v3" />
        </svg>
      );
    case "library":
      // 05 · 对开书
      return (
        <svg {...common}>
          <path d="M4 5a2 2 0 0 1 2-2h4v18H6a2 2 0 0 0-2 2V5zM14 3h4a2 2 0 0 1 2 2v16a2 2 0 0 0-2-2h-4V3z" />
        </svg>
      );
    case "analytics":
      // 06 · 柱图
      return (
        <svg {...common}>
          <path d="M4 20V10M10 20V4M16 20v-8M22 20H2" />
        </svg>
      );
  }
}
