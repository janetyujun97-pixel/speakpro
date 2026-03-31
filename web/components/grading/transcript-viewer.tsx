import { cn } from "@/lib/utils";

interface TranscriptWord {
  text: string;
  isError?: boolean;
  errorType?: "pronunciation" | "grammar" | "vocabulary";
}

interface TranscriptSegment {
  speaker: "student" | "system";
  words: TranscriptWord[];
  timestamp: string;
}

interface TranscriptViewerProps {
  segments?: TranscriptSegment[];
  className?: string;
}

const sampleSegments: TranscriptSegment[] = [
  {
    speaker: "system",
    timestamp: "0:00",
    words: [
      { text: "Please" },
      { text: "describe" },
      { text: "your" },
      { text: "favorite" },
      { text: "holiday" },
      { text: "destination." },
    ],
  },
  {
    speaker: "student",
    timestamp: "0:05",
    words: [
      { text: "My" },
      { text: "favorite" },
      { text: "holiday" },
      { text: "destination" },
      { text: "is" },
      { text: "Japan." },
      { text: "I" },
      { text: "have" },
      { text: "went", isError: true, errorType: "grammar" },
      { text: "there" },
      { text: "last" },
      { text: "year." },
      { text: "The" },
      { text: "food" },
      { text: "is" },
      { text: "very" },
      { text: "delicous.", isError: true, errorType: "pronunciation" },
    ],
  },
];

const errorColors = {
  pronunciation: "bg-data-red/10 text-data-red border-b-2 border-data-red",
  grammar: "bg-data-orange/10 text-data-orange border-b-2 border-data-orange",
  vocabulary: "bg-data-blue/10 text-data-blue border-b-2 border-data-blue",
};

export function TranscriptViewer({
  segments = sampleSegments,
  className,
}: TranscriptViewerProps) {
  return (
    <div className={cn("rounded-xl border border-border bg-white p-4 space-y-4", className)}>
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-primary">语音转写</h3>
        <div className="flex gap-3 text-xs">
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-data-red" />
            发音错误
          </span>
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-data-orange" />
            语法错误
          </span>
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-data-blue" />
            词汇错误
          </span>
        </div>
      </div>

      <div className="space-y-3">
        {segments.map((segment, i) => (
          <div key={i} className="flex gap-3">
            <div className="flex flex-col items-center">
              <span
                className={cn(
                  "inline-flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-medium",
                  segment.speaker === "system"
                    ? "bg-data-blue/10 text-data-blue"
                    : "bg-accent/10 text-accent"
                )}
              >
                {segment.speaker === "system" ? "S" : "学"}
              </span>
              <span className="mt-1 text-[10px] text-muted-foreground">
                {segment.timestamp}
              </span>
            </div>
            <p className="flex-1 leading-relaxed text-sm">
              {segment.words.map((word, j) => (
                <span
                  key={j}
                  className={cn(
                    "inline-block mr-1",
                    word.isError &&
                      word.errorType &&
                      errorColors[word.errorType]
                  )}
                  title={
                    word.isError && word.errorType
                      ? `${word.errorType} error`
                      : undefined
                  }
                >
                  {word.text}
                </span>
              ))}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
