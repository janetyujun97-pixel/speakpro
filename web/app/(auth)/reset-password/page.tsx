"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { resetEmailPassword } from "@/lib/auth";

function ResetPasswordForm() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const canSubmit =
    token.length > 0 &&
    password.length >= 6 &&
    password === confirm;

  if (!token) {
    return (
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>链接无效</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            缺少 token 参数。请重新申请重置链接。
          </p>
          <Link href="/forgot-password">
            <Button className="w-full">重新申请</Button>
          </Link>
        </CardContent>
      </Card>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;
    setLoading(true);
    setError("");
    try {
      await resetEmailPassword(token, password);
      setDone(true);
      setTimeout(() => router.push("/login"), 1500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "重置失败，请重试");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="items-center">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-accent text-xl font-bold text-white">
          SP
        </div>
        <CardTitle className="text-2xl">设置新密码</CardTitle>
        <p className="text-sm text-muted-foreground">
          为账号设置一个新密码
        </p>
      </CardHeader>
      <CardContent>
        {done ? (
          <div className="space-y-4">
            <div className="rounded-lg bg-green-50 p-4 text-sm text-green-900 dark:bg-green-950/30 dark:text-green-200">
              <p className="font-medium">密码已更新。</p>
              <p className="mt-1">正在跳转到登录页…</p>
            </div>
            <Link href="/login">
              <Button variant="outline" className="w-full">
                立即登录
              </Button>
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="password" className="text-sm font-medium">
                新密码
              </label>
              <Input
                id="password"
                type="password"
                placeholder="至少 6 位"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="confirm" className="text-sm font-medium">
                确认密码
              </label>
              <Input
                id="confirm"
                type="password"
                placeholder="再次输入新密码"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
                minLength={6}
              />
              {confirm.length > 0 && password !== confirm && (
                <p className="text-xs text-red-500">两次输入的密码不一致</p>
              )}
            </div>
            {error && <p className="text-sm text-red-500">{error}</p>}
            <Button
              type="submit"
              className="w-full"
              disabled={loading || !canSubmit}
            >
              {loading ? "更新中..." : "更新密码"}
            </Button>
          </form>
        )}
      </CardContent>
    </Card>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="text-sm text-muted-foreground">加载中...</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
