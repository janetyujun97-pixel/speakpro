"use client";

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { ArrowRight, Eye, EyeOff } from "lucide-react";
import { login } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  HairlineBtn,
} from "@/components/editorial/primitives";

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center"><Mono size={11}>— 加载中 —</Mono></div>}>
      <LoginInner />
    </Suspense>
  );
}

function LoginInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const from = searchParams.get("from") || "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      await login({ email, password });
      router.push(from);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败，请重试");
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
          <Serif size={32} weight={500}>Edition</Serif>
          <Serif size={32} italic color="var(--accent)">
            Login
          </Serif>
        </div>
        <div className="mt-1.5">
          <Mono size={10}>登录您的教师账号</Mono>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="mt-9 space-y-5">
        <div>
          <Eyebrow>邮箱</Eyebrow>
          <input
            id="email"
            type="email"
            placeholder="teacher@speakpro.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            className="mt-2 w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
            style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
          />
        </div>

        <div>
          <Eyebrow>密码</Eyebrow>
          <div className="relative mt-2">
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="请输入密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              className="w-full border-0 border-b border-ink bg-transparent pb-1.5 pr-7 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute bottom-1.5 right-0 text-muted hover:text-ink transition-colors"
              tabIndex={-1}
            >
              {showPassword
                ? <EyeOff className="h-[16px] w-[16px]" strokeWidth={1.3} />
                : <Eye className="h-[16px] w-[16px]" strokeWidth={1.3} />}
            </button>
          </div>
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
          disabled={loading}
          style={{ width: "100%", justifyContent: "center" }}
          rightIcon={
            <ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />
          }
        >
          {loading ? "登录中…" : "登录"}
        </HairlineBtn>

        {/* main PR2d 提供的注册 / 忘记密码 —— 以 editorial 样式呈现 */}
        <div className="flex justify-between pt-1">
          <Link href="/register" className="text-[12px] text-muted hover:text-ink transition-colors">
            注册教师账号 →
          </Link>
          <Link href="/forgot-password" className="text-[12px] text-muted hover:text-ink transition-colors">
            忘记密码？
          </Link>
        </div>
      </form>

      <div className="mt-8 text-center">
        <Mono size={9}>EST. 2026 · TEACHING STUDIO</Mono>
      </div>
    </div>
  );
}
