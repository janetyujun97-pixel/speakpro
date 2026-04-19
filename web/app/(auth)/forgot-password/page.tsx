"use client";

import { useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { requestEmailReset } from "@/lib/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");

  const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    setLoading(true);
    setError("");
    try {
      await requestEmailReset(email.trim().toLowerCase());
      setSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "请求失败，请重试");
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
        <CardTitle className="text-2xl">找回密码</CardTitle>
        <p className="text-sm text-muted-foreground">
          输入注册邮箱，我们会发送重置链接
        </p>
      </CardHeader>
      <CardContent>
        {sent ? (
          <div className="space-y-4">
            <div className="rounded-lg bg-muted p-4 text-sm text-foreground">
              <p className="font-medium">如果该邮箱已注册，重置链接已发送到：</p>
              <p className="mt-1 font-mono text-muted-foreground">{email}</p>
              <p className="mt-3 text-muted-foreground">
                链接 30 分钟内有效。若未收到，请检查垃圾邮件或重新申请。
              </p>
            </div>
            <Link href="/login">
              <Button variant="outline" className="w-full">
                返回登录
              </Button>
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium">
                邮箱
              </label>
              <Input
                id="email"
                type="email"
                placeholder="teacher@speakpro.com"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            {error && <p className="text-sm text-red-500">{error}</p>}
            <Button
              type="submit"
              className="w-full"
              disabled={loading || !isValid}
            >
              {loading ? "发送中..." : "发送重置链接"}
            </Button>
            <div className="flex justify-between text-sm text-muted-foreground">
              <Link href="/login" className="hover:text-foreground underline">
                返回登录
              </Link>
              <Link href="/register" className="hover:text-foreground underline">
                注册新账号
              </Link>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  );
}
