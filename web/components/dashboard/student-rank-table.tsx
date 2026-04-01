import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface StudentRank {
  rank: number;
  name: string;
  email: string;
  avgScore: number;
  totalSessions: number;
}

interface StudentRankTableProps {
  students: StudentRank[];
}

export function StudentRankTable({ students }: StudentRankTableProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>学生排行</CardTitle>
      </CardHeader>
      <CardContent>
        {students.length === 0 ? (
          <div className="flex items-center justify-center h-[300px] text-muted-foreground text-sm">
            暂无学生数据
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left">
                  <th className="pb-3 font-medium text-muted-foreground">排名</th>
                  <th className="pb-3 font-medium text-muted-foreground">姓名</th>
                  <th className="pb-3 text-right font-medium text-muted-foreground">
                    平均分
                  </th>
                  <th className="pb-3 text-right font-medium text-muted-foreground">
                    练习次数
                  </th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr
                    key={student.rank}
                    className="border-b border-border last:border-0"
                  >
                    <td className="py-3">
                      <span
                        className={
                          student.rank <= 3
                            ? "inline-flex h-6 w-6 items-center justify-center rounded-full bg-accent/10 text-xs font-bold text-accent"
                            : "pl-2 text-muted-foreground"
                        }
                      >
                        {student.rank}
                      </span>
                    </td>
                    <td className="py-3 font-medium text-primary">
                      {student.name}
                    </td>
                    <td className="py-3 text-right font-semibold text-primary">
                      {student.avgScore}
                    </td>
                    <td className="py-3 text-right text-muted-foreground">
                      {student.totalSessions}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
