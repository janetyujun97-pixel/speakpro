"use client";

import { useEffect, useState, useCallback } from "react";
import { Search, Ban, CheckCircle2, ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/lib/api";

interface ManagedUser {
  id: string;
  name: string;
  email: string | null;
  status: "active" | "disabled";
  createdAt: string;
}

interface UserListResp {
  items: ManagedUser[];
  total: number;
  page: number;
  pageSize: number;
}

const PAGE_SIZE = 20;

export default function AdminStudentsPage() {
  const [items, setItems] = useState<ManagedUser[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [msg, setMsg] = useState("");

  const fetchList = useCallback(async (p: number, query: string) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ role: "student", page: String(p), pageSize: String(PAGE_SIZE) });
      if (query.trim()) params.set("q", query.trim());
      const data = await api.get<UserListResp>(`/users?${params.toString()}`);
      setItems(data.items || []);
      setTotal(data.total || 0);
      setPage(data.page || p);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载学生列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList(1, "");
  }, [fetchList]);

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchList(1, q);
  };

  const toggleStatus = async (u: ManagedUser) => {
    const next = u.status === "active" ? "disabled" : "active";
    if (next === "disabled" && !confirm(`确认禁用学生 "${u.name}"？禁用后无法登录 App。`)) return;
    try {
      await api.put(`/users/${u.id}/status`, { status: next });
      setMsg(next === "disabled" ? "已禁用" : "已启用");
      await fetchList(page, q);
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败");
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-primary">学生管理</h1>
          <p className="mt-1 text-sm text-muted-foreground">共 {total} 名学生 · 学生通过手机 App 自助注册</p>
        </div>
        <form onSubmit={onSearch} className="flex items-center gap-2">
          <div className="flex items-center gap-2 rounded-lg border border-border bg-white px-3 py-2">
            <Search className="h-4 w-4 text-muted-foreground" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="搜索姓名 / 邮箱"
              className="w-44 bg-transparent text-sm outline-none"
            />
          </div>
          <button type="submit" className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-muted">
            搜索
          </button>
        </form>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}
      {msg && !error && (
        <div className="mb-4 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{msg}</div>
      )}

      {/* Table */}
      <div className="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="px-5 py-3 font-medium">姓名</th>
              <th className="px-5 py-3 font-medium">邮箱</th>
              <th className="px-5 py-3 font-medium">状态</th>
              <th className="px-5 py-3 font-medium">注册时间</th>
              <th className="px-5 py-3 text-right font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            {loading && items.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-5 py-10 text-center text-muted-foreground">加载中…</td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-5 py-10 text-center text-muted-foreground">暂无学生</td>
              </tr>
            ) : (
              items.map((u) => (
                <tr key={u.id} className="border-b border-border last:border-b-0 hover:bg-muted/40">
                  <td className="px-5 py-3 font-medium text-primary">{u.name}</td>
                  <td className="px-5 py-3 text-muted-foreground">{u.email || "—"}</td>
                  <td className="px-5 py-3">
                    {u.status === "active" ? (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">正常</span>
                    ) : (
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">已禁用</span>
                    )}
                  </td>
                  <td className="px-5 py-3 text-muted-foreground">
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString("zh-CN") : "—"}
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex items-center justify-end">
                      <button
                        onClick={() => toggleStatus(u)}
                        title={u.status === "active" ? "禁用" : "启用"}
                        className="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-accent"
                      >
                        {u.status === "active" ? <Ban className="h-4 w-4" /> : <CheckCircle2 className="h-4 w-4" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="mt-4 flex items-center justify-between">
        <span className="text-xs text-muted-foreground">第 {page} / {totalPages} 页</span>
        <div className="flex items-center gap-2">
          <button
            onClick={() => page > 1 && fetchList(page - 1, q)}
            disabled={page <= 1}
            className="flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm text-primary transition-colors hover:bg-muted disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" /> 上一页
          </button>
          <button
            onClick={() => page < totalPages && fetchList(page + 1, q)}
            disabled={page >= totalPages}
            className="flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm text-primary transition-colors hover:bg-muted disabled:opacity-40"
          >
            下一页 <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
