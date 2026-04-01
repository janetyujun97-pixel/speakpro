import { type LucideIcon, Package, FileText, Users, BookOpen } from "lucide-react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

/** 通用空状态组件 — 图标 + 标题 + 描述 + 操作按钮 */
export function EmptyState({
  icon: Icon = Package,
  title,
  description,
  actionLabel,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-white py-16 px-8 text-center",
        className
      )}
    >
      <div className="mb-4 rounded-full bg-muted p-4">
        <Icon className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="text-base font-semibold text-primary mb-1">{title}</h3>
      {description && (
        <p className="text-sm text-muted-foreground max-w-sm mb-4">
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="px-4 py-2 text-sm font-medium text-white bg-accent rounded-lg hover:bg-accent/90 transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}

// 预设空状态
export function EmptyQuestions({ onAction }: { onAction?: () => void }) {
  return (
    <EmptyState
      icon={FileText}
      title="暂无题目"
      description="题库为空，点击下方按钮创建第一个题目"
      actionLabel="创建题目"
      onAction={onAction}
    />
  );
}

export function EmptyAssignments({ onAction }: { onAction?: () => void }) {
  return (
    <EmptyState
      icon={BookOpen}
      title="暂无作业"
      description="还没有创建任何作业，开始布置作业吧"
      actionLabel="创建作业"
      onAction={onAction}
    />
  );
}

export function EmptyClasses({ onAction }: { onAction?: () => void }) {
  return (
    <EmptyState
      icon={Users}
      title="暂无班级"
      description="创建您的第一个班级，开始管理学生"
      actionLabel="创建班级"
      onAction={onAction}
    />
  );
}
