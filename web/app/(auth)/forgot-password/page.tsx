"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, ArrowLeft } from "lucide-react";
import { requestEmailReset } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  HairlineBtn,
} from "@/components/editorial/primitives";

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
    <div className="w-full max-w-md border border-line bg-ivory px-10 py-12">
      {/* Masthead */}
      <div className="text-center">
        <Eyebrow>SPEAKPRO · TEACHER</Eyebrow>
        <div className="mt-2.5 flex items-baseline justify-center gap-2">
          <Serif size={32} weight={500}>Recover</Serif>
          <Serif size={32} italic color="var(--accent)">
            Account
          </Serif>
        </div>
        <div className="mt-1.5">
          <Mono size={10}>输入注册邮箱，收取重置链接</Mono>
        </div>
      </div>

      {sent ? (
        <div className="mt-9 space-y-5">
          <div className="border border-line bg-bg-soft p-5">
            <Eyebrow>已发送 · SENT</Eyebrow>
            <div className="mt-2.5">
              <Serif size={17}>如果该邮箱已注册</Serif>
            </div>
            <div className="mt-1">
              <Mono size={12} color="var(--ink)">{email}</Mono>
            </div>
            <p className="mt-3 text-[12px] leading-relaxed text-muted">
              重置链接已发送，30 分钟内有效。若未收到，请检查垃圾邮件或稍后重新申请。
            </p>
          </div>

          <Link href="/login" className="block">
            <HairlineBtn
              style={{ width: "100%", justifyContent: "center" }}
              leftIcon={<ArrowLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />}
            >
              返回登录
            </HairlineBtn>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-9 space-y-5">
          <div>
            <Eyebrow>邮箱</Eyebrow>
            <input
              id="email"
              type="email"
              placeholder="teacher@speakpro.com"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="mt-2 w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
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
            disabled={loading || !isValid}
            style={{ width: "100%", justifyContent: "center" }}
            rightIcon={
              <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
            }
          >
            {loading ? "发送中…" : "发送重置链接"}
          </HairlineBtn>

          <div className="flex justify-between pt-1">
            <Link href="/login" className="text-[12px] text-muted hover:text-ink transition-colors">
              ← 返回登录
            </Link>
            <Link href="/register" className="text-[12px] text-muted hover:text-ink transition-colors">
              注册新账号 →
            </Link>
          </div>
        </form>
      )}

      <div className="mt-8 text-center">
        <Mono size={9}>EST. 2026 · TEACHING STUDIO</Mono>
      </div>
    </div>
  );
}
