"use client";

import { useEffect, useState } from "react";
import { Plus, X, Loader2, Ban, CheckCircle2, KeyRound } from "lucide-react";
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
  role: string;
  status: "active" | "disabled";
  createdAt: string;
  classCount?: number;
}

interface UserListResp {
  items: ManagedUser[];
  total: number;
  page: number;
  pageSize: number;
}

export default function AdminTeachersPage() {
  const [items, setItems] = useState<ManagedUser[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [msg, setMsg] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [creating, setCreating] = useState(false);

  const fetchList = async () => {
    setLoading(true);
    try {
      const data = await api.get<UserListResp>("/users?role=teacher&pageSize=100");
      setItems(data.items || []);
      setTotal(data.total || 0);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载教师列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || password.length < 6) {
      setError("请填写姓名、邮箱，密码至少 6 位");
      return;
    }
    setCreating(true);
    setError("");
    setMsg("");
    try {
      await api.post("/users", { name: name.trim(), email: email.trim(), password, role: "teacher" });
      setName("");
      setEmail("");
      setPassword("");
      setShowForm(false);
      setMsg("教师账号已创建");
      await fetchList();
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建失败");
    } finally {
      setCreating(false);
    }
  };

  const toggleStatus = async (u: ManagedUser) => {
    const next = u.status === "active" ? "disabled" : "active";
    if (next === "disabled" && !confirm(`确认禁用教师 "${u.name}"？禁用后无法登录。`)) return;
    try {
      await api.put(`/users/${u.id}/status`, { status: next });
      setMsg(next === "disabled" ? "已禁用" : "已启用");
      await fetchList();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败");
    }
  };

  const resetPassword = async (u: ManagedUser) => {
    const pwd = window.prompt(`为教师 "${u.name}" 设置新密码（至少 6 位）：`);
    if (!pwd) return;
    if (pwd.length < 6) {
      setError("密码至少 6 位");
      return;
    }
    try {
      await api.put(`/users/${u.id}/reset-password`, { newPassword: pwd });
      setMsg(`已重置 ${u.name} 的密码`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "重置失败");
    }
  };

  return (
    <div className="mx-auto max-w-4xl">
      {/* Masthead */}
      <div className="mb-8 flex items-end justify-between border-b border-line pb-4">
        <div>
          <Eyebrow>管理 · ADMIN</Eyebrow>
          <div className="mt-1">
            <Serif size={28}>教师管理</Serif>
          </div>
          <div className="mt-1">
            <Mono size={10}>共 {total} 名教师 · 由管理员创建</Mono>
          </div>
        </div>
        <HairlineBtn
          primary
          onClick={() => setShowForm((v) => !v)}
          leftIcon={
            showForm ? (
              <X className="h-[13px] w-[13px]" strokeWidth={1.5} />
            ) : (
              <Plus className="h-[13px] w-[13px]" strokeWidth={1.5} />
            )
          }
        >
          {showForm ? "取消" : "创建教师"}
        </HairlineBtn>
      </div>

      {/* Create form */}
      {showForm && (
        <form onSubmit={handleCreate} className="mb-6 flex flex-wrap items-end gap-4 border border-line bg-ivory p-5">
          <div className="min-w-[150px] flex-1">
            <Eyebrow>姓名</Eyebrow>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="如：李老师"
              required
              className="mt-1.5 w-full border-0 border-b border-ink bg-transparent pb-1 text-[14px] text-ink outline-none"
            />
          </div>
          <div className="min-w-[200px] flex-1">
            <Eyebrow>邮箱</Eyebrow>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="teacher@example.com"
              required
              className="mt-1.5 w-full border-0 border-b border-ink bg-transparent pb-1 text-[14px] text-ink outline-none"
            />
          </div>
          <div className="min-w-[150px] flex-1">
            <Eyebrow>初始密码</Eyebrow>
            <input
              type="text"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="至少 6 位"
              required
              className="mt-1.5 w-full border-0 border-b border-ink bg-transparent pb-1 text-[14px] text-ink outline-none"
            />
          </div>
          <HairlineBtn
            primary
            type="submit"
            disabled={creating}
            leftIcon={creating ? <Loader2 className="h-[13px] w-[13px] animate-spin" /> : undefined}
          >
            确认创建
          </HairlineBtn>
        </form>
      )}

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
        <div className="grid grid-cols-[1.4fr_2fr_0.8fr_0.8fr_1fr] items-center border-b border-line px-5 py-3">
          <Eyebrow>姓名</Eyebrow>
          <Eyebrow>邮箱</Eyebrow>
          <Eyebrow>班级数</Eyebrow>
          <Eyebrow>状态</Eyebrow>
          <Eyebrow>操作</Eyebrow>
        </div>
        {loading && items.length === 0 ? (
          <div className="py-12 text-center"><Mono size={11}>— 加载中 —</Mono></div>
        ) : items.length === 0 ? (
          <div className="py-12 text-center"><Mono size={11}>— 暂无教师，点击右上角创建 —</Mono></div>
        ) : (
          items.map((u) => (
            <div
              key={u.id}
              className="grid grid-cols-[1.4fr_2fr_0.8fr_0.8fr_1fr] items-center border-b border-line px-5 py-3 last:border-b-0"
            >
              <div className="text-[14px] font-medium text-ink">{u.name}</div>
              <Mono size={11}>{u.email || "—"}</Mono>
              <span className="font-mono text-[13px] text-ink">{u.classCount ?? 0}</span>
              <div>
                {u.status === "active" ? <Chip tone="moss">正常</Chip> : <Chip tone="warn">已禁用</Chip>}
              </div>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => resetPassword(u)}
                  title="重置密码"
                  className="p-1.5 text-muted transition-colors hover:text-ink"
                >
                  <KeyRound className="h-4 w-4" strokeWidth={1.3} />
                </button>
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
    </div>
  );
}
