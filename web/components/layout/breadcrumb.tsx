"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronRight, Home } from "lucide-react";

const pathNameMap: Record<string, string> = {
  "": "数据看板",
  resources: "资源管理",
  questions: "题库",
  library: "教学资源",
  assignments: "作业管理",
  new: "新建",
  grade: "批改",
  classes: "班级管理",
  students: "学生",
  settings: "设置",
};

export function Breadcrumb() {
  const pathname = usePathname();
  const segments = pathname.split("/").filter(Boolean);

  if (segments.length === 0) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Home className="h-4 w-4" />
        <span className="font-medium text-primary">数据看板</span>
      </div>
    );
  }

  return (
    <nav className="flex items-center gap-1 text-sm">
      <Link
        href="/"
        className="text-muted-foreground transition-colors hover:text-primary"
      >
        <Home className="h-4 w-4" />
      </Link>
      {segments.map((segment, index) => {
        const href = `/${segments.slice(0, index + 1).join("/")}`;
        const isLast = index === segments.length - 1;
        const label = pathNameMap[segment] || segment;

        return (
          <div key={href} className="flex items-center gap-1">
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
            {isLast ? (
              <span className="font-medium text-primary">{label}</span>
            ) : (
              <Link
                href={href}
                className="text-muted-foreground transition-colors hover:text-primary"
              >
                {label}
              </Link>
            )}
          </div>
        );
      })}
    </nav>
  );
}
