"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import FileUpload from "@/components/ui/file-upload";

interface QuestionData {
  id: string;
  examType: string;
  section: string;
  topic: string;
  promptText: string;
  difficulty: number;
  tags: string[];
  sampleAudioUrl: string | null;
}

export default function EditQuestionPage() {
  const params = useParams();
  const router = useRouter();
  const questionId = params.id as string;

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const [examType, setExamType] = useState("IELTS");
  const [section, setSection] = useState("");
  const [topic, setTopic] = useState("");
  const [promptText, setPromptText] = useState("");
  const [difficulty, setDifficulty] = useState(3);
  const [tags, setTags] = useState("");
  const [sampleAudioUrl, setSampleAudioUrl] = useState("");

  useEffect(() => {
    loadQuestion();
  }, [questionId]);

  async function loadQuestion() {
    try {
      const data = await api.get<QuestionData>(`/questions/${questionId}`);
      setExamType(data.examType || "IELTS");
      setSection(data.section || "");
      setTopic(data.topic || "");
      setPromptText(data.promptText || "");
      setDifficulty(data.difficulty || 3);
      setTags((data.tags || []).join(", "));
      setSampleAudioUrl(data.sampleAudioUrl || "");
      setLoading(false);
    } catch (err) {
      setError("加载题目失败");
      setLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");

    try {
      await api.put(`/questions/${questionId}`, {
        examType,
        section,
        topic,
        promptText,
        difficulty,
        tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
        sampleAudioUrl: sampleAudioUrl || null,
      });
      router.push("/resources/questions");
    } catch (err) {
      setError("保存失败，请重试");
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-gray-500">加载中...</p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">编辑题目</h1>

      {error && (
        <div className="bg-red-50 text-red-600 p-3 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        {/* 考试类型 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            考试类型 *
          </label>
          <select
            value={examType}
            onChange={(e) => setExamType(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="TOEFL">TOEFL</option>
            <option value="IELTS">IELTS</option>
          </select>
        </div>

        {/* 题型 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            题型 (Section) *
          </label>
          <input
            type="text"
            value={section}
            onChange={(e) => setSection(e.target.value)}
            placeholder="如: Part1, ReadAloud, FollowRead"
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>

        {/* 话题 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            话题 (Topic)
          </label>
          <input
            type="text"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* 题目内容 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            题目内容 *
          </label>
          <textarea
            value={promptText}
            onChange={(e) => setPromptText(e.target.value)}
            rows={5}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>

        {/* 难度 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            难度
          </label>
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(Number(e.target.value))}
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            {[1, 2, 3, 4, 5].map((d) => (
              <option key={d} value={d}>
                {d} - {["入门", "基础", "中等", "进阶", "挑战"][d - 1]}
              </option>
            ))}
          </select>
        </div>

        {/* 标签 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            标签（逗号分隔）
          </label>
          <input
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="如: 日常话题, 环境, 科技"
            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* 音频样本上传 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            音频样本
          </label>
          {sampleAudioUrl && (
            <div className="mb-2 p-2 bg-gray-50 rounded text-sm text-gray-600 flex items-center justify-between">
              <span className="truncate">{sampleAudioUrl}</span>
              <button
                type="button"
                onClick={() => setSampleAudioUrl("")}
                className="text-red-500 text-xs ml-2 shrink-0"
              >
                移除
              </button>
            </div>
          )}
          <FileUpload
            accept="audio/*,.mp3,.wav,.m4a"
            maxSizeMB={20}
            label="上传音频样本（可选）"
            onUploadComplete={(url) => setSampleAudioUrl(url)}
            onError={(err) => setError(err)}
          />
        </div>

        {/* 操作按钮 */}
        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? "保存中..." : "保存修改"}
          </button>
          <button
            type="button"
            onClick={() => router.back()}
            className="px-6 py-2 border rounded-lg hover:bg-gray-50"
          >
            取消
          </button>
        </div>
      </form>
    </div>
  );
}
