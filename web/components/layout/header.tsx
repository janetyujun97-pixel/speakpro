"use client";

import { usePathname } from "next/navigation";
import { Bell, Search } from "lucide-react";
import { Serif, Mono } from "@/components/editorial/primitives";

// 页面 meta 映射 — 对齐 design/SpeakPro Admin/shell.jsx PAGE_META
interface PageMeta {
  title: string;
  subtitle?: string;
  breadcrumbs: string[];
}

function resolvePageMeta(pathname: string): PageMeta {
  const rules: Array<[(p: string) => boolean, PageMeta]> = [
    [(p) => p === "/", { title: "数据看板", subtitle: "Overview", breadcrumbs: ["HOME"] }],
    [(p) => p.startsWith("/classes"), { title: "班级管理", subtitle: "Classes", breadcrumbs: ["HOME", "CLASSES"] }],
    // 批改中心优先：/assignments/*/grade 或 /grading 都算
    [(p) => p.endsWith("/grade") || p.startsWith("/grading"), { title: "批改中心", subtitle: "Grading", breadcrumbs: ["HOME", "GRADING"] }],
    [(p) => p.startsWith("/assignments"), { title: "作业管理", subtitle: "Assignments", breadcrumbs: ["HOME", "ASSIGNMENTS"] }],
    [(p) => p.startsWith("/resources"), { title: "题库与资源", subtitle: "Library", breadcrumbs: ["HOME", "LIBRARY"] }],
    [(p) => p.startsWith("/analytics"), { title: "学情分析", subtitle: "Analytics", breadcrumbs: ["HOME", "ANALYTICS"] }],
    [(p) => p.startsWith("/system"), { title: "系统管理", subtitle: "System", breadcrumbs: ["HOME", "SYSTEM"] }],
    [(p) => p.startsWith("/settings"), { title: "个人设置", subtitle: "Settings", breadcrumbs: ["HOME", "SETTINGS"] }],
  ];
  for (const [match, meta] of rules) if (match(pathname)) return meta;
  return { title: "SpeakPro", breadcrumbs: ["HOME"] };
}

export function Header() {
  const pathname = usePathname();
  const meta = resolvePageMeta(pathname);

  return (
    <header className="flex items-center gap-8 border-b border-line bg-bg px-10 pb-5 pt-[22px]">
      <div className="min-w-0 flex-1">
        {/* 面包屑 */}
        <div className="mb-1.5 flex items-center gap-2">
          {meta.breadcrumbs.map((b, i) => {
            const last = i === meta.breadcrumbs.length - 1;
            return (
              <span key={i} className="flex items-center gap-2">
                <Mono size={10} color={last ? "var(--ink)" : "var(--muted)"}>{b}</Mono>
                {!last && <Mono size={10}>/</Mono>}
              </span>
            );
          })}
        </div>
        {/* 标题 + 副标题 */}
        <div className="flex items-baseline gap-3.5">
          <Serif as="h1" size={34} weight={400}>{meta.title}</Serif>
          {meta.subtitle && (
            <Serif size={20} italic color="var(--muted)">{meta.subtitle}</Serif>
          )}
        </div>
      </div>

      {/* 搜索 */}
      <div className="flex w-[280px] items-center gap-2 border border-line bg-ivory px-3.5 py-2">
        <Search className="h-[15px] w-[15px] text-muted" strokeWidth={1.3} />
        <input
          placeholder="搜索学生、班级或题目…"
          className="flex-1 border-0 bg-transparent text-[12px] text-ink outline-none placeholder:text-muted"
        />
        <span className="rounded-[2px] border border-line px-1.5 py-0.5">
          <Mono size={9}>⌘K</Mono>
        </span>
      </div>

      {/* 消息铃 */}
      <button
        className="relative flex h-10 w-10 items-center justify-center border border-line bg-ivory"
        aria-label="通知"
      >
        <Bell className="h-[17px] w-[17px] text-ink" strokeWidth={1.3} />
        <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-accent" />
      </button>
    </header>
  );
}
