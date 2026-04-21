"use client";

// /grading 是 Sidebar 04 「批改中心」的入口 —— 没有独立页面，
// 挑一份最合适的作业跳转到它的 grade 页。优先"待批改最多"，
// 无待批改时退而求其次选最近创建的作业（让用户至少看到批改 UI 的空状态），
// 完全无作业时才回退到作业列表。
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Mono } from "@/components/editorial/primitives";

interface Assignment {
  id: string;
  createdAt?: string;
  dueDate?: string;
  submissions: { status: string }[] | null;
}

export default function GradingIndexPage() {
  const router = useRouter();

  useEffect(() => {
    (async () => {
      try {
        const list = await api.get<Assignment[]>("/assignments");

        if (!list || list.length === 0) {
          router.replace("/assignments");
          return;
        }

        // 1) 优先：待批改数量最多、截止日期最近
        const withPending = list
          .map((a) => ({
            a,
            pending:
              (a.submissions || []).filter((s) => s.status === "submitted").length,
            due: a.dueDate ? new Date(a.dueDate).getTime() : Infinity,
          }))
          .filter((x) => x.pending > 0)
          .sort((x, y) => y.pending - x.pending || x.due - y.due);

        if (withPending[0]) {
          router.replace(`/assignments/${withPending[0].a.id}/grade`);
          return;
        }

        // 2) 次选：最近创建的作业（让用户看到批改 UI 空态）
        const sorted = [...list].sort(
          (a, b) =>
            new Date(b.createdAt || 0).getTime() -
            new Date(a.createdAt || 0).getTime()
        );
        router.replace(`/assignments/${sorted[0].id}/grade`);
      } catch {
        router.replace("/assignments");
      }
    })();
  }, [router]);

  return (
    <div className="py-20 text-center">
      <Mono size={11}>— 正在进入批改中心 —</Mono>
    </div>
  );
}
