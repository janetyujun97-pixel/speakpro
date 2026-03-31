export default function GradePage({
  params,
}: {
  params: { id: string };
}) {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">作业批改</h1>
      <p className="text-sm text-muted-foreground">作业 ID: {params.id}</p>
      <div className="rounded-xl border border-border bg-white p-12 text-center text-muted-foreground">
        作业批改页面开发中...
      </div>
    </div>
  );
}
