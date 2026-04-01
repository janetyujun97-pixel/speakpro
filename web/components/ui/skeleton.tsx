import { cn } from "@/lib/utils";

interface SkeletonProps {
  className?: string;
}

/** 通用骨架屏占位元素 + shimmer 动画 */
export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        "animate-shimmer rounded-md bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%]",
        className
      )}
    />
  );
}

/** 骨架屏 — 统计卡片 */
export function SkeletonStatsCard() {
  return (
    <div className="rounded-xl border border-border bg-white p-5 space-y-3">
      <Skeleton className="h-4 w-20" />
      <Skeleton className="h-8 w-16" />
    </div>
  );
}

/** 骨架屏 — 表格行 */
export function SkeletonTableRow({ cols = 4 }: { cols?: number }) {
  return (
    <div className="flex gap-4 px-4 py-3">
      {Array.from({ length: cols }).map((_, i) => (
        <Skeleton
          key={i}
          className={cn("h-4", i === 0 ? "w-32" : "w-20")}
        />
      ))}
    </div>
  );
}

/** 骨架屏 — 图表区域 */
export function SkeletonChart() {
  return (
    <div className="rounded-xl border border-border bg-white p-4 space-y-3">
      <Skeleton className="h-5 w-24" />
      <Skeleton className="h-[280px] w-full rounded-lg" />
    </div>
  );
}

/** 骨架屏 — 卡片网格 */
export function SkeletonCardGrid({ count = 6 }: { count?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="rounded-xl border border-border bg-white p-5 space-y-3">
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-4 w-24" />
        </div>
      ))}
    </div>
  );
}
