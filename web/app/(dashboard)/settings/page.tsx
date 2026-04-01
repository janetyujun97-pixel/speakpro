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
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const user = getUser();
    if (user) {
      setName(user.name || "");
      setEmail(user.email || "");
    }
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
