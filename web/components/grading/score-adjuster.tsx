"use client";

import { useState, useEffect } from "react";
import { cn } from "@/lib/utils";

export interface ScoreCategory {
  label: string;
  key: string;
  aiScore: number;
  maxScore: number;
}

interface ScoreAdjusterProps {
  categories: ScoreCategory[];
  className?: string;
  onScoresChange?: (scores: Record<string, number>, totalScore: number) => void;
}

export function ScoreAdjuster({
  categories,
  className,
  onScoresChange,
}: ScoreAdjusterProps) {
  const [scores, setScores] = useState<Record<string, number>>(
    Object.fromEntries(categories.map((c) => [c.key, c.aiScore]))
  );

  // 当 categories 变化时重置分数（例如切换学生）
  useEffect(() => {
    const newScores = Object.fromEntries(categories.map((c) => [c.key, c.aiScore]));
    setScores(newScores);
  }, [categories]);

  const handleScoreChange = (key: string, value: number) => {
    const newScores = { ...scores, [key]: value };
    setScores(newScores);
    const total = Math.round(
      Object.values(newScores).reduce((sum, s) => sum + s, 0) / categories.length
    );
    onScoresChange?.(newScores, total);
  };

  const totalScore = Math.round(
    Object.values(scores).reduce((sum, s) => sum + s, 0) / (categories.length || 1)
  );

  if (!categories || categories.length === 0) {
    return (
      <div className={cn("rounded-xl border border-border bg-white p-4", className)}>
        <h3 className="font-semibold text-primary mb-2">评分调整</h3>
        <p className="text-sm text-muted-foreground text-center py-6">暂无 AI 评分数据</p>
      </div>
    );
  }

  return (
    <div className={cn("rounded-xl border border-border bg-white p-4 space-y-5", className)}>
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-primary">评分调整</h3>
        <div className="text-right">
          <span className="text-xs text-muted-foreground">综合得分</span>
          <p className="text-2xl font-bold text-accent">{totalScore}</p>
        </div>
      </div>

      <div className="space-y-4">
        {categories.map((category) => (
          <div key={category.key} className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">{category.label}</span>
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground">
                  AI: {category.aiScore}
                </span>
                <span className="min-w-[2.5rem] text-right font-semibold text-primary">
                  {scores[category.key]}
                </span>
              </div>
            </div>
            <input
              type="range"
              min={0}
              max={category.maxScore}
              value={scores[category.key]}
              onChange={(e) =>
                handleScoreChange(category.key, Number(e.target.value))
              }
              className="w-full accent-accent"
            />
          </div>
        ))}
      </div>
    </div>
  );
}
