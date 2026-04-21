"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import { register } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  HairlineBtn,
} from "@/components/editorial/primitives";

export default function RegisterPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const canSubmit =
    name.trim().length > 0 &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) &&
    password.length >= 6 &&
    password === confirm;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) {
      setError("请检查表单：邮箱格式、密码至少 6 位且两次一致");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await register({ name: name.trim(), email: email.trim().toLowerCase(), password });
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "注册失败，请重试");
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
            New
          </Serif>
        </div>
        <div className="mt-1.5">
          <Mono size={10}>创建教师账号 · 开始使用</Mono>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="mt-9 space-y-5">
        <EditField
          id="name"
          label="姓名"
          type="text"
          placeholder="王老师"
          autoComplete="name"
          value={name}
          onChange={setName}
        />

        <EditField
          id="email"
          label="邮箱"
          type="email"
          placeholder="teacher@speakpro.com"
          autoComplete="email"
          value={email}
          onChange={setEmail}
        />

        <EditField
          id="password"
          label="密码"
          type="password"
          placeholder="至少 6 位"
          autoComplete="new-password"
          value={password}
          onChange={setPassword}
        />

        <div>
          <EditField
            id="confirm"
            label="确认密码"
            type="password"
            placeholder="再次输入密码"
            autoComplete="new-password"
            value={confirm}
            onChange={setConfirm}
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
          {loading ? "注册中…" : "创建账号"}
        </HairlineBtn>

        <div className="flex justify-between pt-1">
          <Link href="/login" className="text-[12px] text-muted hover:text-ink transition-colors">
            ← 已有账号？登录
          </Link>
        </div>
      </form>

      <div className="mt-8 text-center">
        <Mono size={9}>EST. 2026 · TEACHING STUDIO</Mono>
      </div>
    </div>
  );
}

// ── 下划线式 serif 输入字段（与 login 保持一致） ────────────────────

function EditField({
  id,
  label,
  type,
  placeholder,
  autoComplete,
  value,
  onChange,
}: {
  id: string;
  label: string;
  type: string;
  placeholder?: string;
  autoComplete?: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div>
      <Eyebrow>{label}</Eyebrow>
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required
        className="mt-2 w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none placeholder:text-muted-2"
        style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
      />
    </div>
  );
}
