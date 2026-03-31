export default function StudentDetailPage({
  params,
}: {
  params: { id: string; sid: string };
}) {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-primary">学生详情</h1>
      <p className="text-sm text-muted-foreground">
        班级 ID: {params.id} / 学生 ID: {params.sid}
      </p>
      <div className="rounded-xl border border-border bg-white p-12 text-center text-muted-foreground">
        学生详情页面开发中...
      </div>
    </div>
  );
}
