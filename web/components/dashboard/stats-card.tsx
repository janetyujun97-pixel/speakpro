import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { TrendingUp, TrendingDown, Minus, type LucideIcon } from "lucide-react";

interface StatsCardProps {
  title: string;
  value: string | number;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
  icon: LucideIcon;
  className?: string;
}

const trendConfig = {
  up: { icon: TrendingUp, color: "text-success", label: "上升" },
  down: { icon: TrendingDown, color: "text-data-red", label: "下降" },
  neutral: { icon: Minus, color: "text-muted-foreground", label: "持平" },
};

export function StatsCard({
  title,
  value,
  trend = "neutral",
  trendValue,
  icon: Icon,
  className,
}: StatsCardProps) {
  const trendInfo = trendConfig[trend];
  const TrendIcon = trendInfo.icon;

  return (
    <Card className={cn("", className)}>
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-2">
            <p className="text-sm text-muted-foreground">{title}</p>
            <p className="text-3xl font-bold text-primary">{value}</p>
            {trendValue && (
              <div className={cn("flex items-center gap-1 text-xs", trendInfo.color)}>
                <TrendIcon className="h-3 w-3" />
                <span>{trendValue}</span>
              </div>
            )}
          </div>
          <div className="rounded-lg bg-accent/10 p-3">
            <Icon className="h-5 w-5 text-accent" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
