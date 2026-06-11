"use client";

import { useEffect, useState } from "react";
import { Plus, X, Loader2, Ban, CheckCircle2, KeyRound } from "lucide-react";
import { api } from "@/lib/api";

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
    <div>
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">教师管理</h1>
          <p className="mt-1 text-sm text-muted-foreground">共 {total} 名教师 · 教师账号由管理员创建</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-2 rounded-lg bg-accent px-4 py-2 text-sm font-medium text-white transition-colors hover:opacity-90"
        >
          {showForm ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showForm ? "取消" : "创建教师"}
        </button>
      </div>

      {/* Create form */}
      {showForm && (
        <form onSubmit={handleCreate} className="mb-6 rounded-xl border border-border bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-sm text-muted-foreground">姓名</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="如：李老师"
                required
                className="w-full rounded-lg border border-border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-accent"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm text-muted-foreground">邮箱</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="teacher@example.com"
                required
                className="w-full rounded-lg border border-border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-accent"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm text-muted-foreground">初始密码</label>
              <input
                type="text"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="至少 6 位"
                required
                className="w-full rounded-lg border border-border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-accent"
              />
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button
              type="submit"
              disabled={creating}
              className="flex items-center gap-2 rounded-lg bg-accent px-5 py-2 text-sm font-medium text-white transition-colors hover:opacity-90 disabled:opacity-50"
            >
              {creating && <Loader2 className="h-4 w-4 animate-spin" />}
              确认创建
            </button>
          </div>
        </form>
      )}

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
              <th className="px-5 py-3 font-medium">班级数</th>
              <th className="px-5 py-3 font-medium">状态</th>
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
                <td colSpan={5} className="px-5 py-10 text-center text-muted-foreground">暂无教师，点击右上角创建</td>
              </tr>
            ) : (
              items.map((u) => (
                <tr key={u.id} className="border-b border-border last:border-b-0 hover:bg-muted/40">
                  <td className="px-5 py-3 font-medium text-primary">{u.name}</td>
                  <td className="px-5 py-3 text-muted-foreground">{u.email || "—"}</td>
                  <td className="px-5 py-3 text-primary">{u.classCount ?? 0}</td>
                  <td className="px-5 py-3">
                    {u.status === "active" ? (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">正常</span>
                    ) : (
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">已禁用</span>
                    )}
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => resetPassword(u)}
                        title="重置密码"
                        className="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-primary"
                      >
                        <KeyRound className="h-4 w-4" />
                      </button>
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
    </div>
  );
}
