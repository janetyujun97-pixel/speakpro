"use client";

import { useEffect, useState, useCallback } from "react";
import { Search, Ban, CheckCircle2, ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Mono,
  Chip,
  HairlineBtn,
} from "@/components/editorial/primitives";

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
    <div className="mx-auto max-w-4xl">
      {/* Masthead */}
      <div className="mb-8 flex items-end justify-between gap-4 border-b border-line pb-4">
        <div>
          <Eyebrow>管理 · ADMIN</Eyebrow>
          <div className="mt-1">
            <Serif size={28}>学生管理</Serif>
          </div>
          <div className="mt-1">
            <Mono size={10}>共 {total} 名学生 · App 自助注册</Mono>
          </div>
        </div>
        <form onSubmit={onSearch} className="flex items-center gap-2">
          <div className="flex items-center gap-2 border border-line px-3 py-2">
            <Search className="h-[14px] w-[14px] text-muted" strokeWidth={1.4} />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="搜索姓名 / 邮箱"
              className="w-[160px] bg-transparent text-[13px] text-ink outline-none"
            />
          </div>
          <HairlineBtn type="submit">搜索</HairlineBtn>
        </form>
      </div>

      {error && (
        <div className="mb-4 border-l-2 border-accent bg-ivory px-4 py-3 text-[13px]" style={{ color: "var(--accent)" }}>
          {error}
        </div>
      )}
      {msg && !error && (
        <div className="mb-4 border-l-2 bg-ivory px-4 py-3 text-[13px]" style={{ borderColor: "var(--moss)", color: "var(--moss)" }}>
          {msg}
        </div>
      )}

      {/* Table */}
      <div className="border border-line bg-ivory">
        <div className="grid grid-cols-[1.4fr_2fr_0.9fr_1.1fr_0.8fr] items-center border-b border-line px-5 py-3">
          <Eyebrow>姓名</Eyebrow>
          <Eyebrow>邮箱</Eyebrow>
          <Eyebrow>状态</Eyebrow>
          <Eyebrow>注册时间</Eyebrow>
          <Eyebrow>操作</Eyebrow>
        </div>
        {loading && items.length === 0 ? (
          <div className="py-12 text-center"><Mono size={11}>— 加载中 —</Mono></div>
        ) : items.length === 0 ? (
          <div className="py-12 text-center"><Mono size={11}>— 暂无学生 —</Mono></div>
        ) : (
          items.map((u) => (
            <div
              key={u.id}
              className="grid grid-cols-[1.4fr_2fr_0.9fr_1.1fr_0.8fr] items-center border-b border-line px-5 py-3 last:border-b-0"
            >
              <div className="text-[14px] font-medium text-ink">{u.name}</div>
              <Mono size={11}>{u.email || "—"}</Mono>
              <div>
                {u.status === "active" ? <Chip tone="moss">正常</Chip> : <Chip tone="warn">已禁用</Chip>}
              </div>
              <Mono size={10}>{u.createdAt ? new Date(u.createdAt).toLocaleDateString("zh-CN") : "—"}</Mono>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => toggleStatus(u)}
                  title={u.status === "active" ? "禁用" : "启用"}
                  className="p-1.5 text-muted transition-colors hover:text-accent"
                >
                  {u.status === "active" ? <Ban className="h-4 w-4" strokeWidth={1.3} /> : <CheckCircle2 className="h-4 w-4" strokeWidth={1.3} />}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Pagination */}
      <div className="mt-4 flex items-center justify-between">
        <Mono size={10}>第 {page} / {totalPages} 页</Mono>
        <div className="flex items-center gap-2">
          <HairlineBtn
            onClick={() => page > 1 && fetchList(page - 1, q)}
            disabled={page <= 1}
            leftIcon={<ChevronLeft className="h-[13px] w-[13px]" strokeWidth={1.4} />}
          >
            上一页
          </HairlineBtn>
          <HairlineBtn
            onClick={() => page < totalPages && fetchList(page + 1, q)}
            disabled={page >= totalPages}
            rightIcon={<ChevronRight className="h-[13px] w-[13px]" strokeWidth={1.4} />}
          >
            下一页
          </HairlineBtn>
        </div>
      </div>
    </div>
  );
}
