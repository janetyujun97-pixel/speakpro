"use client";

import { useState, useEffect } from "react";
import { Music, FileText, Video, BookOpen, Trash2, Loader2, Plus } from "lucide-react";
import { api } from "@/lib/api";
import FileUpload from "@/components/ui/file-upload";
import { Button } from "@/components/ui/button";

interface ResourceItem {
  id: string;
  title: string;
  type: string;
  fileUrl: string;
  fileSize: number;
  examType: string;
  tags: string[];
  createdAt: string;
}

const TYPE_OPTIONS = [
  { value: "", label: "全部类型" },
  { value: "audio", label: "音频" },
  { value: "document", label: "文档" },
  { value: "video", label: "视频" },
  { value: "wordlist", label: "词表" },
];

const TYPE_ICONS: Record<string, any> = {
  audio: Music,
  document: FileText,
  video: Video,
  wordlist: BookOpen,
};

export default function LibraryPage() {
  const [resources, setResources] = useState<ResourceItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [showUpload, setShowUpload] = useState(false);

  // 新资源表单
  const [newTitle, setNewTitle] = useState("");
  const [newType, setNewType] = useState("audio");
  const [newExamType, setNewExamType] = useState("IELTS");
  const [uploadedUrl, setUploadedUrl] = useState("");

  useEffect(() => {
    loadResources();
  }, [typeFilter]);

  async function loadResources() {
    try {
      const params = new URLSearchParams();
      if (typeFilter) params.set("type", typeFilter);
      const data = await api.get<ResourceItem[]>(
        `/resources${params.toString() ? "?" + params : ""}`
      );
      setResources(Array.isArray(data) ? data : []);
    } catch {
      console.error("加载资源失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateResource() {
    if (!newTitle || !uploadedUrl) return;
    try {
      await api.post("/resources", {
        title: newTitle,
        type: newType,
        fileUrl: uploadedUrl,
        examType: newExamType,
      });
      setShowUpload(false);
      setNewTitle("");
      setUploadedUrl("");
      loadResources();
    } catch {
      alert("创建资源失败");
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("确定要删除该资源吗？")) return;
    try {
      await api.delete(`/resources/${id}`);
      setResources((prev) => prev.filter((r) => r.id !== id));
    } catch {
      alert("删除失败");
    }
  }

  function formatSize(bytes: number) {
    if (!bytes) return "—";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">资源库</h1>
        <button
          onClick={() => setShowUpload(!showUpload)}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          {showUpload ? "取消" : "+ 上传资源"}
        </button>
      </div>

      {showUpload && (
        <div className="bg-white p-6 rounded-xl shadow-sm mb-6 space-y-4">
          <h2 className="font-semibold text-lg">上传新资源</h2>
          <div className="grid grid-cols-3 gap-4">
            <input
              type="text"
              placeholder="资源标题 *"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              className="px-3 py-2 border rounded-lg"
            />
            <select value={newType} onChange={(e) => setNewType(e.target.value)} className="px-3 py-2 border rounded-lg">
              <option value="audio">音频</option>
              <option value="document">文档</option>
              <option value="video">视频</option>
              <option value="wordlist">词表</option>
            </select>
            <select value={newExamType} onChange={(e) => setNewExamType(e.target.value)} className="px-3 py-2 border rounded-lg">
              <option value="IELTS">IELTS</option>
              <option value="TOEFL">TOEFL</option>
            </select>
          </div>
          <FileUpload
            accept={newType === "audio" ? "audio/*" : newType === "video" ? "video/*" : "*"}
            maxSizeMB={50}
            onUploadComplete={(url) => setUploadedUrl(url)}
            onError={(err) => alert(err)}
          />
          {uploadedUrl && (
            <div className="flex justify-end">
              <button onClick={handleCreateResource} disabled={!newTitle} className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50">
                确认创建
              </button>
            </div>
          )}
        </div>
      )}

      <div className="flex gap-2 mb-4">
        {TYPE_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setTypeFilter(opt.value)}
            className={`px-3 py-1 text-sm rounded-full border ${typeFilter === opt.value ? "bg-blue-600 text-white border-blue-600" : "bg-white hover:bg-gray-50"}`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-gray-500 text-center py-10">加载中...</p>
      ) : resources.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-lg">暂无资源</p>
          <p className="text-sm mt-1">点击"上传资源"按钮添加</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {resources.map((r) => (
            <div key={r.id} className="bg-white p-4 rounded-xl shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  {(() => {
                    const Icon = TYPE_ICONS[r.type] || FileText;
                    return (
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10">
                        <Icon className="h-5 w-5 text-accent" />
                      </div>
                    );
                  })()}
                  <div>
                    <h3 className="font-medium text-gray-800 line-clamp-1">{r.title}</h3>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {r.examType} · {formatSize(r.fileSize)} · {new Date(r.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <button onClick={() => handleDelete(r.id)} className="text-gray-400 hover:text-red-500 text-sm">删除</button>
              </div>
              {r.tags?.length > 0 && (
                <div className="flex gap-1 mt-3 flex-wrap">
                  {r.tags.slice(0, 3).map((tag) => (
                    <span key={tag} className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded">{tag}</span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
