"use client";

import { useState, useEffect, useRef } from "react";
import { Bell, User, LogOut, Settings, ChevronDown } from "lucide-react";
import { useRouter } from "next/navigation";
import { Breadcrumb } from "./breadcrumb";
import { getUser, logout } from "@/lib/auth";

export function Header() {
  const [showMenu, setShowMenu] = useState(false);
  const [userName, setUserName] = useState("用户");
  const menuRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  useEffect(() => {
    const user = getUser();
    if (user?.name) setUserName(user.name);
  }, []);

  // 点击外部关闭菜单
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-white px-6">
      <Breadcrumb />

      <div className="flex items-center gap-4">
        {/* Notification bell */}
        <button className="relative rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-primary">
          <Bell className="h-5 w-5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-accent" />
        </button>

        {/* User menu */}
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setShowMenu(!showMenu)}
            className="flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-muted"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-medium text-white">
              <User className="h-4 w-4" />
            </div>
            <span className="text-sm font-medium text-primary">{userName}</span>
            <ChevronDown className={`h-4 w-4 text-muted-foreground transition-transform ${showMenu ? "rotate-180" : ""}`} />
          </button>

          {/* Dropdown */}
          {showMenu && (
            <div className="absolute right-0 top-12 z-50 w-48 rounded-xl border border-border bg-white py-1 shadow-lg">
              <button
                onClick={() => {
                  setShowMenu(false);
                  router.push("/settings");
                }}
                className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Settings className="h-4 w-4 text-gray-400" />
                个人设置
              </button>
              <div className="my-1 border-t border-gray-100" />
              <button
                onClick={() => {
                  setShowMenu(false);
                  logout();
                }}
                className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50"
              >
                <LogOut className="h-4 w-4" />
                退出登录
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
