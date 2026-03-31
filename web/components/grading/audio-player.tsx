"use client";

import { Play, Pause, SkipBack, SkipForward, Volume2 } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

interface AudioPlayerProps {
  src?: string;
  className?: string;
}

export function AudioPlayer({ src, className }: AudioPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const duration = 120; // placeholder duration in seconds

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div
      className={cn(
        "rounded-xl border border-border bg-white p-4 space-y-4",
        className
      )}
    >
      {/* Waveform placeholder */}
      <div className="flex h-16 items-end justify-center gap-[2px] rounded-lg bg-muted/50 px-4">
        {Array.from({ length: 60 }).map((_, i) => (
          <div
            key={i}
            className={cn(
              "w-1 rounded-full transition-colors",
              i < (currentTime / duration) * 60
                ? "bg-accent"
                : "bg-muted-foreground/20"
            )}
            style={{
              height: `${Math.random() * 80 + 20}%`,
            }}
          />
        ))}
      </div>

      {/* Controls */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <button className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-primary">
            <SkipBack className="h-4 w-4" />
          </button>
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-accent text-white hover:bg-accent/90"
          >
            {isPlaying ? (
              <Pause className="h-5 w-5" />
            ) : (
              <Play className="ml-0.5 h-5 w-5" />
            )}
          </button>
          <button className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-primary">
            <SkipForward className="h-4 w-4" />
          </button>
        </div>

        {/* Progress bar */}
        <div className="flex flex-1 items-center gap-3">
          <span className="text-xs text-muted-foreground">
            {formatTime(currentTime)}
          </span>
          <div className="relative flex-1">
            <input
              type="range"
              min={0}
              max={duration}
              value={currentTime}
              onChange={(e) => setCurrentTime(Number(e.target.value))}
              className="w-full accent-accent"
            />
          </div>
          <span className="text-xs text-muted-foreground">
            {formatTime(duration)}
          </span>
        </div>

        <button className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-primary">
          <Volume2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
