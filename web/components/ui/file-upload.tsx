"use client";

import { useState, useRef, useCallback } from "react";
import { uploadFile } from "@/lib/upload";

interface FileUploadProps {
  accept?: string;         // 如 "audio/*,.mp3,.wav"
  maxSizeMB?: number;      // 最大文件大小（MB）
  onUploadComplete?: (url: string) => void;
  onError?: (error: string) => void;
  label?: string;
  className?: string;
}

export default function FileUpload({
  accept = "*",
  maxSizeMB = 50,
  onUploadComplete,
  onError,
  label = "点击或拖拽文件到此处上传",
  className = "",
}: FileUploadProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [fileName, setFileName] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback(
    async (file: File) => {
      // 文件大小校验
      if (file.size > maxSizeMB * 1024 * 1024) {
        onError?.(`文件大小超过限制 (${maxSizeMB}MB)`);
        return;
      }

      setFileName(file.name);
      setIsUploading(true);
      setProgress(30); // 模拟进度

      try {
        const url = await uploadFile(file);
        setProgress(100);
        onUploadComplete?.(url);
      } catch (err) {
        onError?.((err as Error).message || "上传失败");
        setProgress(0);
      } finally {
        setIsUploading(false);
      }
    },
    [maxSizeMB, onUploadComplete, onError]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);
      const file = e.dataTransfer.files[0];
      if (file) handleFile(file);
    },
    [handleFile]
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) handleFile(file);
    },
    [handleFile]
  );

  return (
    <div
      className={`relative border-2 border-dashed rounded-lg p-6 text-center transition-colors cursor-pointer
        ${isDragging ? "border-blue-500 bg-blue-50" : "border-gray-300 hover:border-gray-400"}
        ${className}`}
      onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
      onDragLeave={() => setIsDragging(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
    >
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={handleChange}
      />

      {isUploading ? (
        <div className="space-y-2">
          <p className="text-sm text-gray-600">正在上传 {fileName}...</p>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      ) : fileName && progress === 100 ? (
        <div className="space-y-1">
          <p className="text-sm text-green-600 font-medium">✓ {fileName} 上传成功</p>
          <p className="text-xs text-gray-400">点击重新上传</p>
        </div>
      ) : (
        <div className="space-y-2">
          <svg className="mx-auto h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
          <p className="text-sm text-gray-600">{label}</p>
          <p className="text-xs text-gray-400">最大 {maxSizeMB}MB</p>
        </div>
      )}
    </div>
  );
}
