import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface StudentRank {
  rank: number;
  name: string;
  class: string;
  avgScore: number;
  practiceCount: number;
}

const sampleStudents: StudentRank[] = [
  { rank: 1, name: "李明", class: "高三(1)班", avgScore: 92, practiceCount: 28 },
  { rank: 2, name: "王芳", class: "高三(2)班", avgScore: 89, practiceCount: 25 },
  { rank: 3, name: "张伟", class: "高三(1)班", avgScore: 87, practiceCount: 30 },
  { rank: 4, name: "刘洋", class: "高三(3)班", avgScore: 85, practiceCount: 22 },
  { rank: 5, name: "陈静", class: "高三(2)班", avgScore: 84, practiceCount: 26 },
  { rank: 6, name: "赵强", class: "高三(1)班", avgScore: 82, practiceCount: 20 },
  { rank: 7, name: "孙丽", class: "高三(3)班", avgScore: 80, practiceCount: 18 },
  { rank: 8, name: "周杰", class: "高三(2)班", avgScore: 78, practiceCount: 24 },
];

export function StudentRankTable() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>学生排行</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left">
                <th className="pb-3 font-medium text-muted-foreground">排名</th>
                <th className="pb-3 font-medium text-muted-foreground">姓名</th>
                <th className="pb-3 font-medium text-muted-foreground">班级</th>
                <th className="pb-3 text-right font-medium text-muted-foreground">
                  平均分
                </th>
                <th className="pb-3 text-right font-medium text-muted-foreground">
                  练习次数
                </th>
              </tr>
            </thead>
            <tbody>
              {sampleStudents.map((student) => (
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
                  <td className="py-3 text-muted-foreground">
                    {student.class}
                  </td>
                  <td className="py-3 text-right font-semibold text-primary">
                    {student.avgScore}
                  </td>
                  <td className="py-3 text-right text-muted-foreground">
                    {student.practiceCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
