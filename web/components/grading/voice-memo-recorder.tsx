"use client";

import { useEffect, useRef, useState } from "react";
import { Mic, Play, Pause, Trash2, Check } from "lucide-react";
import { uploadFile } from "@/lib/upload";

interface Props {
  /** 当前已有的录音 URL（服务端返回的 teacher_voice_url） */
  existingUrl?: string | null;
  /** 录音完成并上传成功后回调，传入 OSS URL */
  onUploaded: (url: string) => void;
  /** 用户删除录音时回调；外层把 teacher_voice_url 置 null 再提交 */
  onCleared: () => void;
}

type Phase = "idle" | "recording" | "preview" | "uploading";

/**
 * 老师语音备注录制组件（PR3d）
 *
 * 用 MediaRecorder API 录 webm/opus → Blob 预览 →
 * 点"保存并上传"时走 OSS 签名直传 → 回调父组件把 URL 塞进 grade body。
 *
 * 未配置 OSS 凭证时 `uploadFile` 会抛错；UI 会展示错误文案但不阻塞批改主流程。
 */
export function VoiceMemoRecorder({
  existingUrl,
  onUploaded,
  onCleared,
}: Props) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [elapsed, setElapsed] = useState(0);
  const [blob, setBlob] = useState<Blob | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      mediaRecorderRef.current?.stream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  // 已有录音直接展示 preview
  useEffect(() => {
    if (existingUrl && !blob && phase === "idle") {
      // 展示为"已有"态：允许播放或删除重录
    }
  }, [existingUrl, blob, phase]);

  async function startRecording() {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mr = new MediaRecorder(stream);
      mediaRecorderRef.current = mr;
      chunksRef.current = [];

      mr.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };
      mr.onstop = () => {
        const b = new Blob(chunksRef.current, { type: "audio/webm" });
        setBlob(b);
        setPhase("preview");
        stream.getTracks().forEach((t) => t.stop());
      };

      mr.start();
      setPhase("recording");
      setElapsed(0);
      timerRef.current = setInterval(() => {
        setElapsed((e) => e + 1);
      }, 1000);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "无法访问麦克风，请检查浏览器权限设置",
      );
    }
  }

  function stopRecording() {
    if (timerRef.current) clearInterval(timerRef.current);
    mediaRecorderRef.current?.stop();
  }

  function playPreview() {
    if (!blob) return;
    if (!audioRef.current) {
      audioRef.current = new Audio(URL.createObjectURL(blob));
      audioRef.current.onended = () => setIsPlaying(false);
    }
    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      audioRef.current.play();
      setIsPlaying(true);
    }
  }

  function discardPreview() {
    audioRef.current?.pause();
    audioRef.current = null;
    setBlob(null);
    setPhase("idle");
    setIsPlaying(false);
    setElapsed(0);
  }

  async function uploadAndSave() {
    if (!blob) return;
    setPhase("uploading");
    setError(null);
    try {
      // 转换成 File 再走 OSS 上传
      const filename = `teacher-voice-${Date.now()}.webm`;
      const file = new File([blob], filename, { type: "audio/webm" });
      const url = await uploadFile(file);
      onUploaded(url);
      // 上传成功后回到 idle 态（UI 上 existingUrl 会更新）
      audioRef.current?.pause();
      audioRef.current = null;
      setBlob(null);
      setIsPlaying(false);
      setElapsed(0);
      setPhase("idle");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "上传失败，请重试（或检查 OSS 凭证配置）",
      );
      setPhase("preview");
    }
  }

  function deleteExisting() {
    onCleared();
  }

  // ---- 渲染 ----

  return (
    <div className="rounded-lg border border-border bg-muted/30 p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-medium text-foreground flex items-center gap-2">
          <Mic className="h-4 w-4 text-accent" />
          语音备注（可选）
        </h4>
        {existingUrl && phase === "idle" && !blob && (
          <span className="text-xs text-green-600 flex items-center gap-1">
            <Check className="h-3 w-3" />
            已有录音
          </span>
        )}
      </div>

      {/* 已有录音的播放控件 */}
      {existingUrl && phase === "idle" && !blob && (
        <div className="space-y-2">
          <audio
            src={existingUrl}
            controls
            className="w-full h-10"
            aria-label="老师语音备注"
          />
          <div className="flex gap-2">
            <button
              onClick={() => setPhase("recording") /* 触发 startRecording */}
              className="text-xs px-3 py-1.5 rounded-md bg-muted hover:bg-muted/70"
            >
              重录
            </button>
            <button
              onClick={() => {
                // 再点一次重录才真的开始
                void startRecording();
              }}
              className="hidden"
            />
            <button
              onClick={deleteExisting}
              className="text-xs px-3 py-1.5 rounded-md text-red-600 hover:bg-red-50 flex items-center gap-1"
            >
              <Trash2 className="h-3 w-3" />
              删除
            </button>
          </div>
        </div>
      )}

      {/* 空态：开始录制 */}
      {phase === "idle" && !existingUrl && !blob && (
        <button
          onClick={startRecording}
          className="w-full py-2.5 rounded-md bg-accent text-white text-sm font-medium hover:bg-accent/90 transition-colors flex items-center justify-center gap-2"
        >
          <Mic className="h-4 w-4" />
          开始录制
        </button>
      )}

      {/* 录制中 */}
      {phase === "recording" && (
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500" />
            </span>
            <span className="text-sm text-foreground font-medium">
              正在录音 · {formatTime(elapsed)}
            </span>
          </div>
          <button
            onClick={stopRecording}
            className="w-full py-2.5 rounded-md bg-primary text-white text-sm font-medium hover:bg-primary/90"
          >
            停止
          </button>
        </div>
      )}

      {/* 预览 */}
      {phase === "preview" && blob && (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <button
              onClick={playPreview}
              className="h-9 w-9 rounded-full bg-accent text-white flex items-center justify-center"
              aria-label={isPlaying ? "暂停" : "播放"}
            >
              {isPlaying ? (
                <Pause className="h-4 w-4" />
              ) : (
                <Play className="h-4 w-4" />
              )}
            </button>
            <span className="text-xs text-muted-foreground">
              预览录音 · {formatTime(elapsed)}
            </span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={uploadAndSave}
              className="flex-1 py-2 rounded-md bg-accent text-white text-sm font-medium hover:bg-accent/90"
            >
              保存并上传
            </button>
            <button
              onClick={discardPreview}
              className="py-2 px-3 rounded-md bg-muted text-sm hover:bg-muted/70"
            >
              重录
            </button>
          </div>
        </div>
      )}

      {/* 上传中 */}
      {phase === "uploading" && (
        <p className="text-sm text-muted-foreground">上传中…</p>
      )}

      {/* 错误 */}
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

function formatTime(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
