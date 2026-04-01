"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { UserPlus, UserMinus, ArrowLeft } from "lucide-react";
import { api } from "@/lib/api";

interface ClassDetail {
  id: string;
  name: string;
  examType: string;
  students: Student[];
}

interface Student {
  id: string;
  name: string;
  email: string;
}

export default function ClassStudentsPage() {
  const { id: classId } = useParams() as { id: string };
  const [classData, setClassData] = useState<ClassDetail | null>(null);
  const [allStudents, setAllStudents] = useState<Student[]>([]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, [classId]);

  async function loadData() {
    try {
      const [cls, students] = await Promise.all([
        api.get<ClassDetail>(`/classes/${classId}`),
        api.get<Student[]>("/users?role=student"),
      ]);
      setClassData(cls);
      setAllStudents(students);
    } catch (err) {
      console.error("加载失败:", err);
    } finally {
      setLoading(false);
    }
  }

  async function addStudent() {
    if (!selectedStudentId) return;
    try {
      await api.post(`/classes/${classId}/students`, { studentId: selectedStudentId });
      setSelectedStudentId("");
      setShowAddForm(false);
      await loadData();
    } catch {
      alert("添加学生失败");
    }
  }

  async function removeStudent(studentId: string, name: string) {
    if (!confirm(`确认移除学生 ${name}？`)) return;
    try {
      await api.delete(`/classes/${classId}/students/${studentId}`);
      await loadData();
    } catch {
      alert("移除学生失败");
    }
  }

  if (loading) {
    return <div className="flex justify-center py-16 text-gray-400">加载中...</div>;
  }

  if (!classData) {
    return <div className="text-center py-16 text-gray-400">班级不存在</div>;
  }

  const currentStudentIds = new Set(classData.students.map((s) => s.id));
  const availableStudents = allStudents.filter((s) => !currentStudentIds.has(s.id));

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/classes" className="text-gray-400 hover:text-gray-600">
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-primary">{classData.name}</h1>
          <p className="text-sm text-gray-500">
            {classData.examType} · {classData.students.length} 名学生
          </p>
        </div>
      </div>

      {/* 操作栏 */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
        >
          <UserPlus className="h-4 w-4" />
          添加学生
        </button>
      </div>

      {/* 添加学生表单 */}
      {showAddForm && (
        <div className="bg-white p-4 rounded-xl shadow-sm flex items-center gap-3">
          <select
            value={selectedStudentId}
            onChange={(e) => setSelectedStudentId(e.target.value)}
            className="flex-1 px-3 py-2 border rounded-lg text-sm"
          >
            <option value="">选择学生...</option>
            {availableStudents.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.email})
              </option>
            ))}
          </select>
          <button
            onClick={addStudent}
            disabled={!selectedStudentId}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 text-sm"
          >
            确认添加
          </button>
          <button
            onClick={() => setShowAddForm(false)}
            className="px-4 py-2 text-gray-500 hover:text-gray-700 text-sm"
          >
            取消
          </button>
        </div>
      )}

      {/* 学生列表 */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {classData.students.length === 0 ? (
          <p className="p-8 text-center text-gray-400">暂无学生，点击上方按钮添加</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-500">姓名</th>
                <th className="text-left px-4 py-3 font-medium text-gray-500">邮箱</th>
                <th className="text-right px-4 py-3 font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {classData.students.map((student) => (
                <tr key={student.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">
                    <Link
                      href={`/classes/${classId}/students/${student.id}`}
                      className="text-blue-600 hover:underline"
                    >
                      {student.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{student.email}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => removeStudent(student.id, student.name)}
                      className="inline-flex items-center gap-1 px-2 py-1 text-red-600 hover:bg-red-50 rounded text-xs"
                    >
                      <UserMinus className="h-3.5 w-3.5" />
                      移除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
