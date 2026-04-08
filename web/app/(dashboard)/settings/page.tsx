"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import { getUser } from "@/lib/auth";

export default function SettingsPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [ttsProvider, setTtsProvider] = useState("mimo");
  const [ttsSaving, setTtsSaving] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const user = getUser();
    if (user) {
      setName(user.name || "");
      setEmail(user.email || "");
    }
    // 加载 TTS 设置
    api.get<{ ttsProvider?: string }>("/users/settings").then((data) => {
      if (data?.ttsProvider) setTtsProvider(data.ttsProvider);
    }).catch(() => {});
  }, []);

  async function handleProfileSave() {
    setProfileSaving(true);
    setMessage("");
    try {
      await api.put("/users/profile", { name });
      setMessage("个人信息已更新");
    } catch {
      setMessage("更新失败");
    } finally {
      setProfileSaving(false);
    }
  }

  async function handlePasswordChange() {
    if (newPassword !== confirmPassword) {
      setMessage("两次输入的新密码不一致");
      return;
    }
    if (newPassword.length < 6) {
      setMessage("新密码至少 6 位");
      return;
    }
    setPasswordSaving(true);
    setMessage("");
    try {
      await api.put("/users/password", { currentPassword, newPassword });
      setMessage("密码已修改");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch {
      setMessage("密码修改失败，请检查当前密码");
    } finally {
      setPasswordSaving(false);
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold">设置</h1>

      {message && (
        <div className={`p-3 rounded-lg text-sm ${message.includes("失败") || message.includes("不一致") ? "bg-red-50 text-red-600" : "bg-green-50 text-green-600"}`}>
          {message}
        </div>
      )}

      {/* 个人信息 */}
      <div className="bg-white p-6 rounded-xl shadow-sm space-y-4">
        <h2 className="font-semibold text-lg">个人信息</h2>
        <div>
          <label className="block text-sm text-gray-600 mb-1">姓名</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">邮箱</label>
          <input
            type="email"
            value={email}
            disabled
            className="w-full px-3 py-2 border rounded-lg bg-gray-50 text-gray-500"
          />
          <p className="text-xs text-gray-400 mt-1">邮箱不可修改</p>
        </div>
        <button
          onClick={handleProfileSave}
          disabled={profileSaving}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {profileSaving ? "保存中..." : "保存"}
        </button>
      </div>

      {/* TTS 模型选择 */}
      <div className="bg-white p-6 rounded-xl shadow-sm space-y-4">
        <h2 className="font-semibold text-lg">TTS 语音合成模型</h2>
        <p className="text-sm text-gray-500">选择 AI 口语对话和跟读练习中使用的语音合成引擎</p>

        <div className="space-y-3">
          {[
            { value: "mimo", label: "MiMo-V2-TTS（小米）", desc: "国内可用，自然度高，支持情感控制", badge: "推荐" },
            { value: "fish", label: "Fish Audio (s2-pro)", desc: "国际服务，80+语言支持，需海外网络", badge: "" },
            { value: "xunfei", label: "讯飞 TTS", desc: "国内稳定，基础英文发音", badge: "备选" },
          ].map((item) => (
            <label
              key={item.value}
              className={`flex items-start gap-3 p-4 rounded-lg border-2 cursor-pointer transition-colors ${
                ttsProvider === item.value
                  ? "border-blue-500 bg-blue-50"
                  : "border-gray-200 hover:border-gray-300"
              }`}
            >
              <input
                type="radio"
                name="ttsProvider"
                value={item.value}
                checked={ttsProvider === item.value}
                onChange={(e) => setTtsProvider(e.target.value)}
                className="mt-1 accent-blue-600"
              />
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-sm">{item.label}</span>
                  {item.badge && (
                    <span className={`text-xs px-1.5 py-0.5 rounded ${
                      item.badge === "推荐" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"
                    }`}>{item.badge}</span>
                  )}
                </div>
                <p className="text-xs text-gray-500 mt-0.5">{item.desc}</p>
              </div>
            </label>
          ))}
        </div>

        <button
          onClick={async () => {
            setTtsSaving(true);
            setMessage("");
            try {
              await api.put("/users/settings", { ttsProvider });
              setMessage("TTS 模型已更新");
            } catch {
              setMessage("TTS 设置保存失败");
            } finally {
              setTtsSaving(false);
            }
          }}
          disabled={ttsSaving}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {ttsSaving ? "保存中..." : "保存 TTS 设置"}
        </button>
      </div>

      {/* 修改密码 */}
      <div className="bg-white p-6 rounded-xl shadow-sm space-y-4">
        <h2 className="font-semibold text-lg">修改密码</h2>
        <div>
          <label className="block text-sm text-gray-600 mb-1">当前密码</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">新密码</label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1">确认新密码</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <button
          onClick={handlePasswordChange}
          disabled={passwordSaving || !currentPassword || !newPassword}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {passwordSaving ? "修改中..." : "修改密码"}
        </button>
      </div>
    </div>
  );
}
