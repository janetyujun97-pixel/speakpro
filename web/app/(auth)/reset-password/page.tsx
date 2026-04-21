"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, ArrowLeft } from "lucide-react";
import { resetEmailPassword } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  HairlineBtn,
} from "@/components/editorial/primitives";

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="py-20 text-center">
          <Mono size={11}>— 加载中 —</Mono>
        </div>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const canSubmit = token.length > 0 && password.length >= 6 && password === confirm;

  // 链接无效 —— 没有 token 参数
  if (!token) {
    return (
      <div className="w-full max-w-md border border-line bg-ivory px-10 py-12">
        <div className="text-center">
          <Eyebrow color="var(--accent)">链接无效 · INVALID</Eyebrow>
          <div className="mt-2.5">
            <Serif size={28}>Expired or</Serif>
          </div>
          <div>
            <Serif size={28} italic color="var(--accent)">
              missing token.
            </Serif>
          </div>
          <p className="mt-4 text-[13px] leading-relaxed text-muted">
            本次访问缺少有效 token 参数。请从邮件里重新点击最新的重置链接，
            或重新申请一封新邮件。
          </p>
        </div>

        <div className="mt-7 space-y-2.5">
          <Link href="/forgot-password" className="block">
            <HairlineBtn
              primary
              style={{ width: "100%", justifyContent: "center" }}
              rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              重新申请
            </HairlineBtn>
          </Link>
          <Link href="/login" className="block">
            <HairlineBtn
              style={{ width: "100%", justifyContent: "center" }}
              leftIcon={<ArrowLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              返回登录
            </HairlineBtn>
          </Link>
        </div>
      </div>
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
    <div className="w-full max-w-md border border-line bg-ivory px-10 py-12">
      <div className="text-center">
        <Eyebrow>SPEAKPRO · TEACHER</Eyebrow>
        <div className="mt-2.5 flex items-baseline justify-center gap-2">
          <Serif size={32} weight={500}>Set</Serif>
          <Serif size={32} italic color="var(--accent)">
            new password.
          </Serif>
        </div>
        <div className="mt-1.5">
          <Mono size={10}>为账号设置一个新密码</Mono>
        </div>
      </div>

      {done ? (
        <div className="mt-9 space-y-5">
          <div
            className="border-l-2 bg-bg-soft px-5 py-4"
            style={{ borderColor: "var(--moss)" }}
          >
            <Eyebrow color="var(--moss)">已更新 · DONE</Eyebrow>
            <div className="mt-2">
              <Serif size={17}>密码已更新</Serif>
            </div>
            <p className="mt-1.5 text-[12px] text-muted">正在跳转到登录页…</p>
          </div>
          <Link href="/login" className="block">
            <HairlineBtn
              primary
              style={{ width: "100%", justifyContent: "center" }}
              rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              立即登录
            </HairlineBtn>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-9 space-y-5">
          <div>
            <Eyebrow>新密码</Eyebrow>
            <input
              id="password"
              type="password"
              placeholder="至少 6 位"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              className="mt-2 w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
          </div>

          <div>
            <Eyebrow>确认密码</Eyebrow>
            <input
              id="confirm"
              type="password"
              placeholder="再次输入新密码"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              minLength={6}
              className="mt-2 w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
            {confirm.length > 0 && password !== confirm && (
              <div className="mt-1.5">
                <Mono size={10} color="var(--accent)">两次输入的密码不一致</Mono>
              </div>
            )}
          </div>

          {error && (
            <div
              className="border-l-2 border-accent bg-bg px-3 py-2 text-[12px]"
              style={{ color: "var(--accent)" }}
            >
              {error}
            </div>
          )}

          <HairlineBtn
            primary
            type="submit"
            disabled={loading || !canSubmit}
            style={{ width: "100%", justifyContent: "center" }}
            rightIcon={
              <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
            }
          >
            {loading ? "更新中…" : "更新密码"}
          </HairlineBtn>
        </form>
      )}

      <div className="mt-8 text-center">
        <Mono size={9}>EST. 2026 · TEACHING STUDIO</Mono>
      </div>
    </div>
  );
}
