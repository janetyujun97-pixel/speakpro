"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { getUser } from "@/lib/auth";
import {
  Eyebrow,
  Serif,
  Mono,
  Chip,
  HairlineBtn,
  SectionRule,
} from "@/components/editorial/primitives";

// ── AI 偏好分组 ─────────────────────────────────────────────────────

const PROVIDER_GROUPS = [
  {
    key: "asrProvider" as const,
    title: "ASR 语音识别",
    desc: "口语转写使用的识别引擎",
    options: [
      { value: "tencent", label: "腾讯云实时 ASR", desc: "国内低延迟，英文模型新版", badge: "推荐" },
      { value: "xunfei",  label: "讯飞 ASR",       desc: "国内稳定备选",               badge: "备选" },
    ],
  },
  {
    key: "iseProvider" as const,
    title: "ISE 发音评测",
    desc: "跟读/朗读练习的发音打分引擎",
    options: [
      { value: "tencent", label: "腾讯云 SOE", desc: "支持流利度 / 完整度 / 重音 / 语调", badge: "推荐" },
      { value: "xunfei",  label: "讯飞 ISE",   desc: "单词级评测，国内稳定",             badge: "备选" },
    ],
  },
  {
    key: "llmProvider" as const,
    title: "LLM 大模型",
    desc: "对话生成与写作评析使用的模型",
    options: [
      { value: "mimo", label: "MiMo-V2-Pro（小米）", desc: "推理质量高，支持流式输出", badge: "推荐" },
      { value: "qwen", label: "通义千问",            desc: "阿里云备选",               badge: "备选" },
    ],
  },
  {
    key: "ttsProvider" as const,
    title: "TTS 语音合成",
    desc: "AI 口语对话和跟读练习的语音合成引擎",
    options: [
      { value: "mimo",   label: "MiMo-V2-TTS（小米）", desc: "国内可用，自然度高，支持情感控制", badge: "推荐" },
      { value: "fish",   label: "Fish Audio (s2-pro)", desc: "国际服务，80+ 语言，需海外网络",   badge: ""     },
      { value: "xunfei", label: "讯飞 TTS",            desc: "国内稳定，基础英文发音",           badge: "备选" },
    ],
  },
] as const;

type ProviderKey = (typeof PROVIDER_GROUPS)[number]["key"];
type AiPrefs = Record<ProviderKey, string>;

const DEFAULT_PREFS: AiPrefs = {
  asrProvider: "tencent",
  iseProvider: "tencent",
  llmProvider: "mimo",
  ttsProvider: "mimo",
};

// ── 页面 ────────────────────────────────────────────────────────────

export default function SettingsPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<string>("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [prefs, setPrefs] = useState<AiPrefs>(DEFAULT_PREFS);

  const [aiSaving, setAiSaving] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [message, setMessage] = useState<{ tone: "ok" | "err"; text: string } | null>(null);

  useEffect(() => {
    const user = getUser();
    if (user) {
      setName(user.name || "");
      setEmail(user.email || "");
      setRole(user.role || "");
    }
    api
      .get<Partial<AiPrefs>>("/users/settings")
      .then((data) => {
        if (data) {
          setPrefs({
            asrProvider: data.asrProvider || DEFAULT_PREFS.asrProvider,
            iseProvider: data.iseProvider || DEFAULT_PREFS.iseProvider,
            llmProvider: data.llmProvider || DEFAULT_PREFS.llmProvider,
            ttsProvider: data.ttsProvider || DEFAULT_PREFS.ttsProvider,
          });
        }
      })
      .catch(() => {});
  }, []);

  async function handleProfileSave() {
    setProfileSaving(true);
    setMessage(null);
    try {
      await api.put("/users/profile", { name });
      setMessage({ tone: "ok", text: "个人信息已更新" });
    } catch {
      setMessage({ tone: "err", text: "更新失败" });
    } finally {
      setProfileSaving(false);
    }
  }

  async function handlePasswordChange() {
    if (newPassword !== confirmPassword) {
      setMessage({ tone: "err", text: "两次输入的新密码不一致" });
      return;
    }
    if (newPassword.length < 6) {
      setMessage({ tone: "err", text: "新密码至少 6 位" });
      return;
    }
    setPasswordSaving(true);
    setMessage(null);
    try {
      await api.put("/users/password", { currentPassword, newPassword });
      setMessage({ tone: "ok", text: "密码已修改" });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch {
      setMessage({ tone: "err", text: "密码修改失败，请检查当前密码" });
    } finally {
      setPasswordSaving(false);
    }
  }

  async function handleAiSave() {
    setAiSaving(true);
    setMessage(null);
    try {
      await api.put("/users/settings", prefs);
      setMessage({ tone: "ok", text: "AI 模型偏好已更新" });
    } catch {
      setMessage({ tone: "err", text: "AI 模型偏好保存失败" });
    } finally {
      setAiSaving(false);
    }
  }

  const roleLabel =
    role === "admin" ? "管理员" : role === "teacher" ? "教师" : role === "student" ? "学生" : "";

  return (
    <div className="mx-auto max-w-3xl">
      {/* Masthead */}
      <div className="mb-8 flex items-baseline justify-between border-b border-line pb-4">
        <div>
          <Eyebrow>账号 · ACCOUNT</Eyebrow>
          <div className="mt-1">
            <Serif size={28}>{name || "—"}</Serif>
          </div>
          <div className="mt-1 flex items-center gap-2">
            {roleLabel && <Chip tone="muted">{roleLabel}</Chip>}
            <Mono size={10}>{email}</Mono>
          </div>
        </div>
      </div>

      {message && (
        <div
          className="mb-6 border-l-2 bg-ivory px-4 py-3 text-[13px]"
          style={{
            borderColor: message.tone === "ok" ? "var(--moss)" : "var(--accent)",
            color: message.tone === "ok" ? "var(--moss)" : "var(--accent)",
          }}
        >
          {message.text}
        </div>
      )}

      {/* ── 01 个人信息 ───────────────────────── */}
      <Section num="01" title="个人信息" subtitle="Profile">
        <Field label="姓名">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[18px] text-ink outline-none"
            style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
          />
        </Field>
        <Field label="邮箱">
          <div className="flex items-center gap-2">
            <Mono size={13} color="var(--ink)">{email}</Mono>
            <Chip tone="muted">不可修改</Chip>
          </div>
        </Field>
        <div className="flex justify-end">
          <HairlineBtn primary onClick={handleProfileSave} disabled={profileSaving}>
            {profileSaving ? "保存中…" : "保存"}
          </HairlineBtn>
        </div>
      </Section>

      {/* ── 02 AI 模型偏好 ─────────────────────── */}
      <Section num="02" title="AI 模型偏好" subtitle="Models">
        <p className="text-[12px] leading-relaxed text-muted">
          自定义四类 AI 引擎。默认推荐项已在系统启用；切换为备选项时，如当前环境未配置对应密钥，将自动降级到可用提供商。
        </p>

        {PROVIDER_GROUPS.map((group, gi) => (
          <div
            key={group.key}
            className="pt-5"
            style={{ borderTop: gi > 0 ? "1px solid var(--line-soft)" : "none", marginTop: gi > 0 ? 20 : 12 }}
          >
            <div className="mb-3">
              <Serif size={15}>{group.title}</Serif>
              <div className="mt-0.5">
                <Mono size={10}>{group.desc}</Mono>
              </div>
            </div>
            <div className="space-y-2">
              {group.options.map((opt) => {
                const on = prefs[group.key] === opt.value;
                return (
                  <label
                    key={opt.value}
                    className="flex cursor-pointer items-start gap-3 px-4 py-3 transition-colors"
                    style={{
                      border: `2px solid ${on ? "var(--accent)" : "var(--line)"}`,
                      background: on ? "var(--accent-soft)" : "transparent",
                    }}
                  >
                    <input
                      type="radio"
                      name={group.key}
                      value={opt.value}
                      checked={on}
                      onChange={(e) =>
                        setPrefs((prev) => ({ ...prev, [group.key]: e.target.value }))
                      }
                      className="mt-1 accent-accent"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-[13px] font-semibold text-ink">{opt.label}</span>
                        {opt.badge && (
                          <Chip tone={opt.badge === "推荐" ? "moss" : "muted"}>{opt.badge}</Chip>
                        )}
                      </div>
                      <div className="mt-0.5">
                        <Mono size={10}>{opt.desc}</Mono>
                      </div>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>
        ))}

        <div className="flex justify-end pt-3">
          <HairlineBtn primary onClick={handleAiSave} disabled={aiSaving}>
            {aiSaving ? "保存中…" : "保存 AI 偏好"}
          </HairlineBtn>
        </div>
      </Section>

      {/* ── 03 修改密码 ─────────────────────────── */}
      <Section num="03" title="修改密码" subtitle="Security">
        <Field label="当前密码">
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            autoComplete="current-password"
            className="w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[16px] text-ink outline-none"
            style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
          />
        </Field>
        <div className="grid grid-cols-2 gap-6">
          <Field label="新密码">
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
              className="w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[16px] text-ink outline-none"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
          </Field>
          <Field label="确认新密码">
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
              className="w-full border-0 border-b border-ink bg-transparent pb-1.5 font-serif text-[16px] text-ink outline-none"
              style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
            />
          </Field>
        </div>
        <div className="flex justify-end">
          <HairlineBtn
            primary
            onClick={handlePasswordChange}
            disabled={passwordSaving || !currentPassword || !newPassword}
          >
            {passwordSaving ? "修改中…" : "修改密码"}
          </HairlineBtn>
        </div>
      </Section>
    </div>
  );
}

// ── helpers ──────────────────────────────────────────────────────────

function Section({
  num,
  title,
  subtitle,
  children,
}: {
  num: string;
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <section className="mb-12">
      <SectionRule
        className="mb-6"
        label={
          <span className="flex items-baseline gap-2">
            <Serif size={14} italic color="var(--muted-2)">
              {num}
            </Serif>
            <span>{title}</span>
            <Mono size={10}>· {subtitle}</Mono>
          </span>
        }
      />
      <div className="space-y-5">{children}</div>
    </section>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <Eyebrow>{label}</Eyebrow>
      <div className="mt-2">{children}</div>
    </div>
  );
}
