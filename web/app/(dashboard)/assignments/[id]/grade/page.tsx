"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "next/navigation";
import { Play, Pause, ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/lib/api";
import {
  Eyebrow,
  Serif,
  Numeral,
  Mono,
  Chip,
  HairlineBtn,
  type ChipTone,
} from "@/components/editorial/primitives";
// main PR3d —— 只引 VoiceMemoRecorder，其它 grading 组件我们这边已重写
import { VoiceMemoRecorder } from "@/components/grading/voice-memo-recorder";

// ── 类型定义 ────────────────────────────────────────────────────────

interface Assignment {
  id: string;
  title: string;
  questionIds: string[];
  submissions: Submission[];
}

interface Submission {
  id: string;
  studentId: string;
  student?: { name: string; email: string };
  sessionIds: string[];
  status: "pending" | "submitted" | "graded";
  teacherScore: number | null;
  teacherComment: string | null;
  teacherVoiceUrl: string | null;
  submittedAt: string | null;
  gradedAt: string | null;
}

// JSONB 子结构（与 go-services/internal/model/types.go 对齐）
interface PronunciationScore {
  overall?: number;
  score?: number;
  fluency?: number;
  integrity?: number;
  stress?: number;
  intonation?: number;
}
interface FluencySubScore {
  score?: number;
  overall?: number;
  pace?: number;
  fillers?: number;
  pauses?: Array<{ time?: number; duration?: number }>;
}
interface GrammarSubScore {
  score?: number;
  overall?: number;
  errors?: Array<{ text?: string; type?: string; suggestion?: string }>;
  corrections?: Array<{ from?: string; to?: string } | string>;
}
interface ContentSubScore {
  score?: number;
  overall?: number;
  relevance?: number;
  vocabulary?: number;
  coherence?: number;
}

interface PracticeSession {
  id: string;
  mode: string;
  audioUrl?: string;
  transcript?: string;
  overallScore?: number;
  pronunciationScore?: PronunciationScore;
  fluencyScore?: FluencySubScore;
  grammarScore?: GrammarSubScore;
  contentScore?: ContentSubScore;
  aiFeedback?: string;
  question?: { promptText?: string; examType?: string; section?: string };
  createdAt: string;
}

// ── 工具 ───────────────────────────────────────────────────────────

const getScore = (obj?: { overall?: number; score?: number }) =>
  obj?.overall ?? obj?.score ?? 0;

/** 0–100 → 0–9（雅思风格展示） */
const to9 = (v: number) => ((v / 100) * 9).toFixed(1);

function formatTime(seconds: number) {
  if (!seconds || !isFinite(seconds)) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function formatDate(iso?: string | null) {
  if (!iso) return "—";
  const d = new Date(iso);
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

// ── 主组件 ─────────────────────────────────────────────────────────

export default function GradePage() {
  const params = useParams();
  const assignmentId = params.id as string;

  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [selectedSubmission, setSelectedSubmission] =
    useState<Submission | null>(null);
  const [sessions, setSessions] = useState<PracticeSession[]>([]);
  const [selectedSession, setSelectedSession] =
    useState<PracticeSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [grading, setGrading] = useState(false);

  const [teacherScore, setTeacherScore] = useState(70);
  const [comment, setComment] = useState("");
  const [teacherVoiceUrl, setTeacherVoiceUrl] = useState<string | null>(null);

  useEffect(() => {
    loadAssignment();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignmentId]);

  async function loadAssignment() {
    try {
      const data = await api.get<Assignment>(`/assignments/${assignmentId}`);
      setAssignment(data);
      const firstPending = data.submissions?.find(
        (s) => s.status === "submitted"
      );
      if (firstPending) selectSubmission(firstPending);
    } catch (err) {
      console.error("加载作业失败:", err);
    } finally {
      setLoading(false);
    }
  }

  async function selectSubmission(sub: Submission) {
    setSelectedSubmission(sub);
    setTeacherScore(sub.teacherScore ?? 70);
    setComment(sub.teacherComment ?? "");
    setTeacherVoiceUrl(sub.teacherVoiceUrl ?? null);
    setSelectedSession(null);
    setSessions([]);

    if (sub.sessionIds?.length > 0) {
      setSessionsLoading(true);
      try {
        const data = await api.post<PracticeSession[]>(
          "/practice/sessions/batch",
          { sessionIds: sub.sessionIds }
        );
        setSessions(data);
        if (data.length > 0) setSelectedSession(data[0]);
      } catch (err) {
        console.error("加载练习记录失败:", err);
      } finally {
        setSessionsLoading(false);
      }
    }
  }

  async function handleGrade() {
    if (!selectedSubmission) return;
    setGrading(true);
    try {
      await api.put(`/assignments/${assignmentId}/grade`, {
        submissionId: selectedSubmission.id,
        teacherScore,
        teacherComment: comment,
        teacherVoiceUrl,
      });
      await loadAssignment();
    } catch {
      alert("评分提交失败");
    } finally {
      setGrading(false);
    }
  }

  if (loading) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 加载中 —</Mono>
      </div>
    );
  }

  if (!assignment) {
    return (
      <div className="py-20 text-center">
        <Mono size={11}>— 作业不存在 —</Mono>
      </div>
    );
  }

  const submittedSubs = (assignment.submissions || []).filter(
    (s) => s.status !== "pending"
  );
  const gradedCount = (assignment.submissions || []).filter(
    (s) => s.status === "graded"
  ).length;

  const curIdx = selectedSubmission
    ? submittedSubs.findIndex((s) => s.id === selectedSubmission.id)
    : -1;
  const hasPrev = curIdx > 0;
  const hasNext = curIdx >= 0 && curIdx < submittedSubs.length - 1;

  return (
    <div className="grid items-start gap-6" style={{ gridTemplateColumns: "260px 1fr" }}>
      {/* ── 队列 ─────────────────────────────────── */}
      <aside>
        <div className="mb-2 flex items-baseline justify-between">
          <Eyebrow>队列 · QUEUE</Eyebrow>
          <Mono size={10}>
            {submittedSubs.length}
            {gradedCount > 0 ? ` · 已批 ${gradedCount}` : ""}
          </Mono>
        </div>
        <div className="border-t border-line">
          {submittedSubs.length === 0 ? (
            <div className="py-8 text-center">
              <Mono size={10}>— 暂无学生提交 —</Mono>
            </div>
          ) : (
            submittedSubs.map((sub, i) => {
              const on = selectedSubmission?.id === sub.id;
              return (
                <button
                  key={sub.id}
                  onClick={() => selectSubmission(sub)}
                  className="flex w-full items-center gap-2.5 border-b border-line px-3 py-3 text-left transition-colors"
                  style={{
                    background: on ? "var(--bg-soft)" : "transparent",
                    borderLeft: `2px solid ${on ? "var(--accent)" : "transparent"}`,
                  }}
                >
                  <Serif
                    size={13}
                    italic
                    color={on ? "var(--accent)" : "var(--muted-2)"}
                  >
                    {String(i + 1).padStart(2, "0")}
                  </Serif>
                  <div className="min-w-0 flex-1">
                    <div className="text-[12px] font-medium text-ink truncate">
                      {sub.student?.name || sub.studentId.slice(0, 8)}
                    </div>
                    <Mono size={9}>
                      {sub.sessionIds?.length || 0} 个练习 ·{" "}
                      {sub.submittedAt
                        ? new Date(sub.submittedAt).toLocaleDateString("zh-CN")
                        : "—"}
                    </Mono>
                  </div>
                  {sub.status === "graded" && sub.teacherScore != null ? (
                    <Numeral size={16} color="var(--muted)">
                      {sub.teacherScore}
                    </Numeral>
                  ) : (
                    <Chip tone="warn">待批</Chip>
                  )}
                </button>
              );
            })
          )}
        </div>
      </aside>

      {/* ── 批改面板 ─────────────────────────────── */}
      <div className="border border-line bg-ivory">
        {selectedSubmission ? (
          <>
            {/* Header */}
            <div className="flex items-start justify-between border-b border-line px-6 py-5">
              <div className="min-w-0 flex-1">
                <div className="mb-2 flex items-center gap-2">
                  <Chip tone="accent">
                    {selectedSession?.question?.section ||
                      selectedSession?.mode ||
                      "作业提交"}
                  </Chip>
                  <Mono size={10}>
                    SUBMITTED · {formatDate(selectedSubmission.submittedAt)}
                  </Mono>
                </div>
                <Serif size={24}>
                  {selectedSession?.question?.promptText || assignment.title}
                </Serif>
                <div className="mt-1.5 text-[12px] text-muted">
                  学生 ·{" "}
                  <span className="font-semibold text-ink">
                    {selectedSubmission.student?.name ||
                      selectedSubmission.studentId.slice(0, 8)}
                  </span>
                  {selectedSession && (
                    <>
                      {" "}
                      · 时长{" "}
                      <span className="text-ink">
                        {formatTime(
                          /* 后端暂无 duration 字段，占位 */ 0
                        )}
                      </span>
                    </>
                  )}
                </div>
              </div>
              <div className="flex gap-2">
                <HairlineBtn
                  disabled={!hasPrev}
                  onClick={() => hasPrev && selectSubmission(submittedSubs[curIdx - 1])}
                  leftIcon={<ChevronLeft className="h-[13px] w-[13px]" strokeWidth={1.3} />}
                  style={{ opacity: hasPrev ? 1 : 0.4 }}
                >
                  上一份
                </HairlineBtn>
                <HairlineBtn
                  disabled={!hasNext}
                  onClick={() => hasNext && selectSubmission(submittedSubs[curIdx + 1])}
                  rightIcon={<ChevronRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
                  style={{ opacity: hasNext ? 1 : 0.4 }}
                >
                  下一份
                </HairlineBtn>
              </div>
            </div>

            {sessionsLoading && (
              <div className="border-b border-line px-6 py-3 text-center">
                <Mono size={11}>加载练习数据…</Mono>
              </div>
            )}

            {/* 会话切换（多练习时显示） */}
            {sessions.length > 1 && (
              <div className="flex flex-wrap gap-2 border-b border-line px-6 py-3">
                <Eyebrow>练习</Eyebrow>
                {sessions.map((s, i) => {
                  const on = selectedSession?.id === s.id;
                  return (
                    <button
                      key={s.id}
                      onClick={() => setSelectedSession(s)}
                      className="border border-line px-2.5 py-1 text-[11px] transition-colors"
                      style={{
                        background: on ? "var(--ink)" : "transparent",
                        color: on ? "var(--ivory)" : "var(--ink)",
                        borderColor: on ? "var(--ink)" : "var(--line)",
                      }}
                    >
                      #{i + 1}
                      {s.overallScore != null && (
                        <span className="ml-1 opacity-70">· {to9(s.overallScore)}</span>
                      )}
                    </button>
                  );
                })}
              </div>
            )}

            {selectedSession ? (
              <>
                {/* 音频 */}
                <AudioBlock src={selectedSession.audioUrl} />

                {/* Transcript + Scoring 双列 */}
                <div
                  className="grid"
                  style={{ gridTemplateColumns: "1.3fr 1fr" }}
                >
                  {/* 转写 */}
                  <div
                    className="px-6 py-5"
                    style={{ borderRight: "1px solid var(--line)" }}
                  >
                    <Eyebrow>转写 · TRANSCRIPT</Eyebrow>
                    <div
                      className="mt-3.5 font-serif text-[15px] leading-[1.75] text-ink"
                      style={{ fontVariationSettings: '"opsz" 144, "SOFT" 50' }}
                    >
                      {selectedSession.transcript || (
                        <Mono size={11}>— 暂无转写 —</Mono>
                      )}
                    </div>
                  </div>

                  {/* 评分 */}
                  <div className="px-6 py-5">
                    <ScorePanel
                      session={selectedSession}
                      teacherScore={teacherScore}
                      onChangeScore={setTeacherScore}
                      comment={comment}
                      onChangeComment={setComment}
                      teacherVoiceUrl={teacherVoiceUrl}
                      onVoiceUrlChange={setTeacherVoiceUrl}
                      grading={grading}
                      alreadyGraded={selectedSubmission.status === "graded"}
                      onSubmit={handleGrade}
                    />
                  </div>
                </div>

                {/* AI 反馈 */}
                {selectedSession.aiFeedback && (
                  <div className="border-t border-line px-6 py-5">
                    <Eyebrow>AI 反馈</Eyebrow>
                    <p className="mt-3 whitespace-pre-wrap text-[13px] leading-relaxed text-ink">
                      {selectedSession.aiFeedback}
                    </p>
                  </div>
                )}
              </>
            ) : (
              !sessionsLoading && (
                <div className="py-16 text-center">
                  <Mono size={11}>— 该提交暂无练习数据 —</Mono>
                </div>
              )
            )}
          </>
        ) : (
          <div className="py-20 text-center">
            <Mono size={11}>— 请从左侧选择一份提交进行批改 —</Mono>
          </div>
        )}
      </div>
    </div>
  );
}

// ── 音频块（内嵌 play/pause + waveform bars + 速度） ────────────────

function AudioBlock({ src }: { src?: string }) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [playing, setPlaying] = useState(false);
  const [current, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const [rate, setRate] = useState(1);

  useEffect(() => {
    const a = audioRef.current;
    if (!a) return;
    const onTime = () => setCurrent(a.currentTime);
    const onDur = () => setDuration(a.duration || 0);
    const onEnd = () => setPlaying(false);
    a.addEventListener("timeupdate", onTime);
    a.addEventListener("durationchange", onDur);
    a.addEventListener("ended", onEnd);
    return () => {
      a.removeEventListener("timeupdate", onTime);
      a.removeEventListener("durationchange", onDur);
      a.removeEventListener("ended", onEnd);
    };
  }, [src]);

  useEffect(() => {
    setCurrent(0);
    setPlaying(false);
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
  }, [src]);

  const toggle = useCallback(() => {
    const a = audioRef.current;
    if (!a || !src) return;
    if (playing) a.pause();
    else a.play();
    setPlaying(!playing);
  }, [playing, src]);

  const changeRate = (r: number) => {
    setRate(r);
    if (audioRef.current) audioRef.current.playbackRate = r;
  };

  // 生成稳定的波形高度（基于 index 的伪随机，不随 render 变化）
  const bars = Array.from({ length: 80 }, (_, i) => {
    const h = 8 + Math.abs(Math.sin(i * 0.5 + Math.cos(i * 0.3) * 2)) * 32;
    return h;
  });
  const played = duration > 0 ? (current / duration) * 80 : 0;

  return (
    <div className="border-b border-line px-6 py-5">
      {src && <audio ref={audioRef} src={src} preload="metadata" />}
      <div className="flex items-center gap-3.5">
        <button
          onClick={toggle}
          disabled={!src}
          className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full transition-opacity disabled:opacity-40"
          style={{ background: "var(--ink)", color: "var(--ivory)" }}
          aria-label={playing ? "暂停" : "播放"}
        >
          {playing ? (
            <Pause className="h-[16px] w-[16px]" fill="currentColor" />
          ) : (
            <Play className="ml-0.5 h-[16px] w-[16px]" fill="currentColor" />
          )}
        </button>

        <div className="flex-1">
          <div className="flex h-11 items-center gap-[2px]">
            {bars.map((h, i) => (
              <div
                key={i}
                className="w-[3px] shrink-0"
                style={{
                  height: h,
                  background: i < played ? "var(--accent)" : "var(--line)",
                }}
              />
            ))}
          </div>
          <div className="mt-1.5 flex justify-between">
            <Mono size={10} color="var(--ink)">
              {formatTime(current)}
            </Mono>
            <Mono size={10}>— {formatTime(duration)} —</Mono>
          </div>
        </div>

        <div className="flex gap-1.5">
          {[0.75, 1, 1.25].map((r) => {
            const on = rate === r;
            return (
              <button
                key={r}
                onClick={() => changeRate(r)}
                className="border border-line px-2 py-1 font-mono text-[10px] transition-colors"
                style={{
                  background: on ? "var(--ink)" : "transparent",
                  color: on ? "var(--ivory)" : "var(--ink)",
                }}
              >
                {r}×
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ── 评分面板（综合分 + 4 分项 + 评语 + 提交） ─────────────────────

function ScorePanel({
  session,
  teacherScore,
  onChangeScore,
  comment,
  onChangeComment,
  teacherVoiceUrl,
  onVoiceUrlChange,
  grading,
  alreadyGraded,
  onSubmit,
}: {
  session: PracticeSession;
  teacherScore: number;
  onChangeScore: (n: number) => void;
  comment: string;
  onChangeComment: (v: string) => void;
  teacherVoiceUrl: string | null;
  onVoiceUrlChange: (v: string | null) => void;
  grading: boolean;
  alreadyGraded: boolean;
  onSubmit: () => void;
}) {
  // 子维度 → 在展开时显示细分详情
  const dims: Array<{
    label: string;
    key: "pronunciation" | "fluency" | "grammar" | "content";
    value: number;
    details?: React.ReactNode;
  }> = [
    {
      label: "发音 Pron.",
      key: "pronunciation",
      value: getScore(session.pronunciationScore),
      details: <PronunciationDetails s={session.pronunciationScore} />,
    },
    {
      label: "流利度 Flu.",
      key: "fluency",
      value: getScore(session.fluencyScore),
      details: <FluencyDetails s={session.fluencyScore} />,
    },
    {
      label: "语法 Gram.",
      key: "grammar",
      value: getScore(session.grammarScore),
      details: <GrammarDetails s={session.grammarScore} />,
    },
    {
      label: "内容 Con.",
      key: "content",
      value: getScore(session.contentScore),
      details: <ContentDetails s={session.contentScore} />,
    },
  ];

  const [expanded, setExpanded] = useState<string | null>(null);
  const overall9 = session.overallScore != null ? to9(session.overallScore) : "—";

  return (
    <div>
      <div className="flex items-center justify-between">
        <Eyebrow>AI 评分 · 可调整</Eyebrow>
        <Mono size={10}>TENCENT SOE</Mono>
      </div>

      {/* 综合分卡片 */}
      <div
        className="mt-3 px-4 pb-3.5 pt-4"
        style={{
          background: "var(--bg-soft)",
          border: "1px solid var(--line)",
        }}
      >
        <div className="flex items-baseline justify-between">
          <Eyebrow>综合分</Eyebrow>
          <Mono size={10}>/ 9.0</Mono>
        </div>
        <div className="mt-1">
          <Numeral size={54} color="var(--accent)">
            {overall9}
          </Numeral>
        </div>
      </div>

      {/* 4 分项 */}
      <div className="mt-4">
        {dims.map((d) => {
          const v9 = to9(d.value);
          const pct = Math.max(0, Math.min(100, d.value));
          const on = expanded === d.key;
          return (
            <div key={d.key} className="border-b border-line-soft py-2.5">
              <button
                onClick={() => setExpanded(on ? null : d.key)}
                className="flex w-full items-center justify-between"
              >
                <span className="text-[12px] text-ink">{d.label}</span>
                <Numeral size={16}>{v9}</Numeral>
              </button>
              <div
                className="relative mt-1.5"
                style={{ height: 2, background: "var(--line-soft)" }}
              >
                <div
                  className="absolute inset-y-0 left-0"
                  style={{ width: `${pct}%`, background: "var(--ink)" }}
                />
              </div>
              {on && d.details && (
                <div className="mt-2 text-[11px] text-muted">{d.details}</div>
              )}
            </div>
          );
        })}
      </div>

      {/* 教师批注 */}
      <div className="mt-4">
        <Eyebrow>教师批注</Eyebrow>
        <textarea
          value={comment}
          onChange={(e) => onChangeComment(e.target.value)}
          placeholder="添加批注或鼓励…"
          rows={3}
          className="mt-2 w-full resize-y border border-line bg-ivory p-3 text-[12px] text-ink outline-none placeholder:text-muted-2"
          style={{ borderRadius: 2, minHeight: 70 }}
        />
      </div>

      {/* 教师语音备注（PR3d） */}
      <div className="mt-4">
        <Eyebrow>语音批注</Eyebrow>
        <div className="mt-2">
          <VoiceMemoRecorder
            existingUrl={teacherVoiceUrl}
            onUploaded={(url) => onVoiceUrlChange(url)}
            onCleared={() => onVoiceUrlChange(null)}
          />
        </div>
      </div>

      {/* 综合评分可调（兼容旧 0-100 teacherScore） */}
      <div className="mt-3">
        <div className="flex items-center justify-between">
          <Eyebrow>教师综合分</Eyebrow>
          <Numeral size={20} color="var(--ink)">
            {teacherScore}
          </Numeral>
        </div>
        <input
          type="range"
          min={0}
          max={100}
          step={1}
          value={teacherScore}
          onChange={(e) => onChangeScore(Number(e.target.value))}
          className="mt-1.5 w-full accent-ink"
        />
      </div>

      {/* 操作按钮 */}
      <div className="mt-4 flex gap-2">
        <HairlineBtn style={{ flex: 1, justifyContent: "center" }}>驳回</HairlineBtn>
        <HairlineBtn
          primary
          disabled={grading}
          onClick={onSubmit}
          style={{ flex: 2, justifyContent: "center" }}
          rightIcon={<ArrowRight className="h-[13px] w-[13px]" strokeWidth={1.3} />}
        >
          {grading ? "提交中…" : alreadyGraded ? "更新评分" : "确认并发布"}
        </HairlineBtn>
      </div>
    </div>
  );
}

// ── 细分详情小组件（沿用上一轮 SOE 展示逻辑，仅换皮） ──────────────

function MiniBar({ label, value }: { label: string; value?: number }) {
  if (value == null) return null;
  const v = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className="flex items-center gap-2">
      <span className="w-14 shrink-0 text-muted">{label}</span>
      <div
        className="relative flex-1"
        style={{ height: 2, background: "var(--line-soft)" }}
      >
        <div
          className="absolute inset-y-0 left-0"
          style={{ width: `${v}%`, background: "var(--accent)" }}
        />
      </div>
      <span className="w-8 text-right font-medium text-ink">{v}</span>
    </div>
  );
}

function PronunciationDetails({ s }: { s?: PronunciationScore }) {
  if (!s) return <span className="text-muted-2">无细分数据</span>;
  const has = s.fluency != null || s.integrity != null || s.stress != null || s.intonation != null;
  if (!has) return <span className="text-muted-2">腾讯 SOE 未返回细分字段</span>;
  return (
    <div className="space-y-1.5">
      <MiniBar label="流利度" value={s.fluency} />
      <MiniBar label="完整度" value={s.integrity} />
      <MiniBar label="重音" value={s.stress} />
      <MiniBar label="语调" value={s.intonation} />
    </div>
  );
}

function FluencyDetails({ s }: { s?: FluencySubScore }) {
  if (!s) return <span className="text-muted-2">无细分数据</span>;
  const pauses = s.pauses || [];
  if (s.pace == null && s.fillers == null && pauses.length === 0) {
    return <span className="text-muted-2">暂无流利度细分</span>;
  }
  return (
    <div className="space-y-1.5">
      {s.pace != null && (
        <div>
          <span className="text-muted">语速：</span>
          <span className="font-medium text-ink">{s.pace.toFixed(0)} 词/分钟</span>
        </div>
      )}
      {s.fillers != null && (
        <div>
          <span className="text-muted">填充词次数：</span>
          <span className="font-medium text-ink">{s.fillers}</span>
        </div>
      )}
      {pauses.length > 0 && (
        <div>
          <p className="mb-1 text-muted">停顿 {pauses.length} 处</p>
          <ul className="max-h-24 space-y-0.5 overflow-y-auto">
            {pauses.slice(0, 10).map((p, i) => (
              <li key={i} className="text-muted">
                · {p.time != null ? `${p.time.toFixed(1)}s` : "?"}，持续{" "}
                {p.duration != null ? `${p.duration.toFixed(2)}s` : "?"}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function GrammarDetails({ s }: { s?: GrammarSubScore }) {
  if (!s) return <span className="text-muted-2">无细分数据</span>;
  const errors = s.errors || [];
  if (errors.length === 0) return <span className="text-muted-2">未发现语法错误</span>;
  return (
    <ul className="max-h-40 space-y-1.5 overflow-y-auto">
      {errors.map((e, i) => (
        <li key={i} className="border-l-2 pl-2" style={{ borderColor: "var(--gold)" }}>
          <div>
            <Chip tone="warn" style={{ marginRight: 6 }}>
              {e.type || "错误"}
            </Chip>
            <span className="text-ink">{e.text}</span>
          </div>
          {e.suggestion && (
            <div className="mt-0.5 text-muted">建议：{e.suggestion}</div>
          )}
        </li>
      ))}
    </ul>
  );
}

function ContentDetails({ s }: { s?: ContentSubScore }) {
  if (!s) return <span className="text-muted-2">无细分数据</span>;
  if (s.relevance == null && s.vocabulary == null && s.coherence == null) {
    return <span className="text-muted-2">暂无内容细分</span>;
  }
  return (
    <div className="space-y-1.5">
      <MiniBar label="相关性" value={s.relevance} />
      <MiniBar label="词汇" value={s.vocabulary} />
      <MiniBar label="连贯性" value={s.coherence} />
    </div>
  );
}
