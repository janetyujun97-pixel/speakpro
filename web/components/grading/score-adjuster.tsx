"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";

interface ScoreCategory {
  label: string;
  key: string;
  aiScore: number;
  maxScore: number;
}

interface ScoreAdjusterProps {
  categories?: ScoreCategory[];
  className?: string;
  onScoresChange?: (scores: Record<string, number>) => void;
}

const defaultCategories: ScoreCategory[] = [
  { label: "发音准确度", key: "pronunciation", aiScore: 82, maxScore: 100 },
  { label: "流利度", key: "fluency", aiScore: 75, maxScore: 100 },
  { label: "语法正确性", key: "grammar", aiScore: 70, maxScore: 100 },
  { label: "词汇丰富度", key: "vocabulary", aiScore: 68, maxScore: 100 },
  { label: "内容相关性", key: "relevance", aiScore: 85, maxScore: 100 },
];

export function ScoreAdjuster({
  categories = defaultCategories,
  className,
  onScoresChange,
}: ScoreAdjusterProps) {
  const [scores, setScores] = useState<Record<string, number>>(
    Object.fromEntries(categories.map((c) => [c.key, c.aiScore]))
  );

  const handleScoreChange = (key: string, value: number) => {
    const newScores = { ...scores, [key]: value };
    setScores(newScores);
    onScoresChange?.(newScores);
  };

  const totalScore = Math.round(
    Object.values(scores).reduce((sum, s) => sum + s, 0) / categories.length
  );

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
