"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { ShieldCheck, Settings as SettingsIcon, LogOut } from "lucide-react";
import { cn } from "@/lib/utils";
import { getUser, logout } from "@/lib/auth";
import { Eyebrow, Serif } from "@/components/editorial/primitives";
import { Logo } from "@/components/brand/logo";
import { NavIcon, type NavIconName } from "@/components/brand/nav-icons";
import type { ComponentType, SVGProps } from "react";

// ── Editorial 编号导航 ───────────────────────────────────────────────
// 对齐 design/SpeakPro Admin/shell.jsx + design/SpeakPro Icons/ Section 04

type BrandIconKey = { kind: "brand"; name: NavIconName };
type LucideIconKey = { kind: "lucide"; component: ComponentType<SVGProps<SVGSVGElement>> };

interface NavEntry {
  num: string;
  key: string;
  label: string;
  en: string;
  href: string;
  icon: BrandIconKey | LucideIconKey;
  matches?: string[];
  matchesEnd?: string[];
  excludesEnd?: string[];
}

const NAV: NavEntry[] = [
  { num: "01", key: "dashboard",   label: "数据看板",   en: "Overview",    href: "/",            icon: { kind: "brand", name: "dashboard"   } },
  { num: "02", key: "classes",     label: "班级管理",   en: "Classes",     href: "/classes",     icon: { kind: "brand", name: "classes"     } },
  { num: "03", key: "assignments", label: "作业管理",   en: "Assignments", href: "/assignments", icon: { kind: "brand", name: "assignments" }, excludesEnd: ["/grade"] },
  { num: "04", key: "grading",     label: "批改中心",   en: "Grading",     href: "/grading",     icon: { kind: "brand", name: "grading"     }, matchesEnd: ["/grade"] },
  { num: "05", key: "resources",   label: "题库与资源", en: "Library",     href: "/resources",   icon: { kind: "brand", name: "library"     }, matches: ["/resources"] },
  { num: "06", key: "analytics",   label: "学情分析",   en: "Analytics",   href: "/analytics",   icon: { kind: "brand", name: "analytics"   } },
];

const ADMIN_NAV: NavEntry = {
  num: "07", key: "system", label: "系统管理", en: "System",
  href: "/system/ai", icon: { kind: "lucide", component: ShieldCheck as ComponentType<SVGProps<SVGSVGElement>> },
  matches: ["/system"],
};

const SETTINGS_NAV: NavEntry = {
  num: "08", key: "settings", label: "个人设置", en: "Settings",
  href: "/settings", icon: { kind: "lucide", component: SettingsIcon as ComponentType<SVGProps<SVGSVGElement>> },
};

/** Masthead 用的 Edition 号 —— 基于当天 day-of-year */
function editionLabel(d: Date) {
  const dayOfYear = Math.floor(
    (d.getTime() - new Date(d.getFullYear(), 0, 0).getTime()) / 86400000
  );
  return {
    no: "№" + String(dayOfYear % 999).padStart(3, "0"),
    dateLine: `${d
      .toLocaleDateString("en-US", { weekday: "short" })
      .toUpperCase()} · ${d
      .toLocaleDateString("en-US", { month: "short" })
      .toUpperCase()} ${d.getDate()}, ${d.getFullYear()}`,
  };
}

export function Sidebar() {
  const pathname = usePathname();
  const [user, setUser] = useState<{ name?: string; email?: string; role?: string } | null>(null);
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    setUser(getUser());
    setNow(new Date());
  }, []);

  const isActive = (entry: NavEntry) => {
    if (entry.href === "/") return pathname === "/";
    if (entry.matchesEnd?.some((s) => pathname.endsWith(s))) return true;
    if (entry.excludesEnd?.some((s) => pathname.endsWith(s))) return false;
    if (entry.matches?.some((p) => pathname.startsWith(p))) return true;
    return pathname.startsWith(entry.href);
  };

  const navItems: NavEntry[] = [
    ...NAV,
    ...(user?.role === "admin" ? [ADMIN_NAV] : []),
    SETTINGS_NAV,
  ];

  const initials = (user?.name || "SP")
    .trim()
    .split(/\s+/)
    .map((s) => s[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  const ed = now ? editionLabel(now) : null;

  return (
    <aside className="flex h-screen w-[252px] flex-shrink-0 flex-col border-r border-line bg-ivory">
      {/* Masthead —— 用 Logo variant="stacked" 复用品牌组件 */}
      <div className="border-b border-line px-6 pb-5 pt-[26px]">
        <Logo
          variant="stacked"
          edition={ed?.no ?? "№—"}
          dateLine={ed?.dateLine ?? ""}
        />
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-[18px]">
        <div className="px-3 pb-2.5">
          <Eyebrow>CONTENTS</Eyebrow>
        </div>
        {navItems.map((item) => {
          const on = isActive(item);
          return (
            <Link
              key={item.key}
              href={item.href}
              className={cn(
                "mb-0.5 flex w-full items-center gap-3 border-l-2 px-3 py-2.5 text-left transition-colors",
                on
                  ? "border-accent bg-bg-soft text-ink"
                  : "border-transparent text-muted hover:text-ink"
              )}
            >
              <Serif
                size={14}
                italic
                color={on ? "var(--accent)" : "var(--muted-2)"}
                style={{ width: 20 }}
              >
                {item.num}
              </Serif>

              {/* 图标 —— 品牌组里的直接渲染 NavIcon，其余用 lucide */}
              <span
                className={cn("flex shrink-0 items-center justify-center", on ? "text-ink" : "text-muted")}
                style={{ width: 17, height: 17 }}
              >
                {item.icon.kind === "brand" ? (
                  <NavIcon name={item.icon.name} size={17} />
                ) : (
                  <item.icon.component
                    width={17}
                    height={17}
                    strokeWidth={1.3}
                  />
                )}
              </span>

              <div className="min-w-0 flex-1">
                <div
                  className={cn(
                    "text-[13px] tracking-[0.015em]",
                    on ? "font-semibold" : "font-medium"
                  )}
                >
                  {item.label}
                </div>
                <div className="mt-px">
                  <Serif size={10} italic color="var(--muted-2)">{item.en}</Serif>
                </div>
              </div>
            </Link>
          );
        })}
      </nav>

      {/* Footer — 当前用户 + 登出 */}
      <button
        onClick={() => logout()}
        className="group flex items-center gap-2.5 border-t border-line p-4 text-left"
      >
        <div
          className="flex h-9 w-9 items-center justify-center rounded-full border border-accent"
          style={{ background: "var(--accent-soft)" }}
        >
          <Serif size={15} italic color="var(--accent)">{initials}</Serif>
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-[12px] font-semibold text-ink">
            {user?.name || "—"}
          </div>
          <div className="truncate text-[10px] tracking-[0.03em] text-muted">
            {user?.role === "admin"
              ? "管理员"
              : user?.role === "teacher"
              ? "教师"
              : user?.role === "student"
              ? "学生"
              : user?.email || ""}
          </div>
        </div>
        <LogOut
          className="h-[15px] w-[15px] text-muted transition-colors group-hover:text-accent"
          strokeWidth={1.3}
        />
      </button>
    </aside>
  );
}

