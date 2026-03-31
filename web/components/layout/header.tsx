"use client";

import { Bell, User } from "lucide-react";
import { Breadcrumb } from "./breadcrumb";

export function Header() {
  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-white px-6">
      <Breadcrumb />

      <div className="flex items-center gap-4">
        {/* Notification bell */}
        <button className="relative rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-primary">
          <Bell className="h-5 w-5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-accent" />
        </button>

        {/* User avatar */}
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-medium text-white">
            <User className="h-4 w-4" />
          </div>
          <span className="text-sm font-medium text-primary">王老师</span>
        </div>
      </div>
    </header>
  );
}
