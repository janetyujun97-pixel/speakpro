"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import {
  LayoutDashboard,
  FolderOpen,
  ClipboardList,
  Users,
  BarChart3,
  Settings as SettingsIcon,
  GraduationCap,
  UserCog,
  LogOut,
} from "lucide-react";
import { getUser, logout } from "@/lib/auth";

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  matches?: string[];
}

// 教师 + 管理员通用导航
const BASE_NAV: NavItem[] = [
  { label: "数据看板", href: "/", icon: LayoutDashboard },
  { label: "资源管理", href: "/resources", icon: FolderOpen, matches: ["/resources"] },
  { label: "作业管理", href: "/assignments", icon: ClipboardList, matches: ["/assignments"] },
  { label: "班级管理", href: "/classes", icon: Users, matches: ["/classes"] },
  { label: "学情分析", href: "/analytics", icon: BarChart3, matches: ["/analytics"] },
];

// 管理员专属导航
const ADMIN_NAV: NavItem[] = [
  { label: "教师管理", href: "/admin/teachers", icon: UserCog, matches: ["/admin/teachers"] },
  { label: "学生管理", href: "/admin/students", icon: GraduationCap, matches: ["/admin/students"] },
];

const SETTINGS_NAV: NavItem = { label: "设置", href: "/settings", icon: SettingsIcon };

export function Sidebar() {
  const pathname = usePathname();
  const [user, setUser] = useState<{ name?: string; email?: string; role?: string } | null>(null);

  useEffect(() => {
    setUser(getUser());
  }, []);

  const isActive = (item: NavItem) => {
    if (item.href === "/") return pathname === "/";
    if (item.matches?.some((p) => pathname.startsWith(p))) return true;
    return pathname.startsWith(item.href);
  };

  const roleLabel =
    user?.role === "admin"
      ? "管理员"
      : user?.role === "teacher"
      ? "教师"
      : user?.role === "student"
      ? "学生"
      : user?.email || "";

  const renderItem = (item: NavItem) => {
    const Icon = item.icon;
    const active = isActive(item);
    return (
      <Link
        key={item.href}
        href={item.href}
        className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors ${
          active
            ? "bg-accent/10 font-medium text-accent"
            : "text-muted-foreground hover:bg-muted hover:text-primary"
        }`}
      >
        <Icon className="h-[18px] w-[18px]" />
        {item.label}
      </Link>
    );
  };

  return (
    <aside className="flex h-screen w-60 flex-shrink-0 flex-col border-r border-border bg-white">
      {/* Logo */}
      <div className="flex items-center gap-2.5 border-b border-border px-5 py-[18px]">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent text-sm font-bold text-white">
          SP
        </div>
        <span className="text-lg font-semibold text-primary">SpeakPro</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {BASE_NAV.map(renderItem)}

        {user?.role === "admin" && (
          <>
            <div className="px-3 pb-1 pt-4">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                系统管理
              </span>
            </div>
            {ADMIN_NAV.map(renderItem)}
          </>
        )}

        <div className="pt-2">{renderItem(SETTINGS_NAV)}</div>
      </nav>

      {/* User footer */}
      <button
        onClick={() => logout()}
        className="group flex items-center gap-3 border-t border-border px-4 py-4 text-left transition-colors hover:bg-muted"
      >
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent/10 text-sm font-semibold text-accent">
          {(user?.name || "SP").slice(0, 1).toUpperCase()}
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-medium text-primary">{user?.name || "—"}</div>
          <div className="truncate text-xs text-muted-foreground">{roleLabel}</div>
        </div>
        <LogOut className="h-4 w-4 text-muted-foreground transition-colors group-hover:text-accent" />
      </button>
    </aside>
  );
}
