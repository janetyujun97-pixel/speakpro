import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Class } from './entities/class.entity';
import { User } from '../users/entities/user.entity';
import { PracticeSession } from '../practice/entities/practice-session.entity';
import { Submission } from '../assignments/entities/submission.entity';

@Injectable()
export class ClassesService {
  constructor(
    @InjectRepository(Class)
    private readonly classesRepository: Repository<Class>,
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
    @InjectRepository(PracticeSession)
    private readonly practiceRepository: Repository<PracticeSession>,
    @InjectRepository(Submission)
    private readonly submissionRepository: Repository<Submission>,
  ) {}

  async create(data: Partial<Class>): Promise<Class> {
    const cls = this.classesRepository.create(data);
    return this.classesRepository.save(cls);
  }

  async findAll(teacherId?: string): Promise<Class[]> {
    const query = this.classesRepository.createQueryBuilder('c')
      .leftJoinAndSelect('c.teacher', 'teacher');

    if (teacherId) {
      query.andWhere('c.teacher_id = :teacherId', { teacherId });
    }

    query.orderBy('c.created_at', 'DESC');
    return query.getMany();
  }

  async findById(id: string): Promise<Class> {
    const cls = await this.classesRepository.findOne({
      where: { id },
      relations: ['teacher', 'students'],
    });
    if (!cls) {
      throw new NotFoundException('班级不存在');
    }
    return cls;
  }

  async update(classId: string, data: { name?: string; examType?: string }): Promise<Class> {
    const cls = await this.findById(classId);
    if (data.name) cls.name = data.name;
    if (data.examType) cls.examType = data.examType as any;
    return this.classesRepository.save(cls);
  }

  async delete(classId: string): Promise<void> {
    const cls = await this.findById(classId);
    await this.classesRepository.remove(cls);
  }

  async removeStudent(classId: string, studentId: string): Promise<Class> {
    const cls = await this.findById(classId);
    cls.students = cls.students.filter((s) => s.id !== studentId);
    await this.classesRepository.save(cls);
    return this.findById(classId);
  }

  async addStudent(classId: string, studentId: string): Promise<Class> {
    const cls = await this.findById(classId);
    const student = await this.usersRepository.findOne({ where: { id: studentId } });
    if (!student) {
      throw new NotFoundException('学生不存在');
    }

    // 避免重复添加
    const alreadyInClass = cls.students.some((s) => s.id === studentId);
    if (!alreadyInClass) {
      cls.students.push(student);
      await this.classesRepository.save(cls);
    }

    return this.findById(classId);
  }

  async getAnalytics(classId: string) {
    const cls = await this.findById(classId);
    const studentIds = cls.students.map((s) => s.id);

    if (studentIds.length === 0) {
      return {
        classId: cls.id,
        className: cls.name,
        studentCount: 0,
        activeStudents: 0,
        averageScore: 0,
        totalSessions: 0,
        assignmentCompletionRate: 0,
        dimensions: { pronunciation: 0, fluency: 0, grammar: 0, content: 0 },
      };
    }

    // 查询班级所有学生的练习数据
    const sessions = await this.practiceRepository
      .createQueryBuilder('ps')
      .where('ps.student_id IN (:...ids)', { ids: studentIds })
      .getMany();

    const scoredSessions = sessions.filter((s) => s.overallScore != null);
    const avgScore =
      scoredSessions.reduce((sum, s) => sum + Number(s.overallScore), 0) /
      (scoredSessions.length || 1);

    // 活跃学生（最近 7 天有练习记录）
    const now = new Date();
    const recentStudents = new Set(
      sessions
        .filter((s) => now.getTime() - new Date(s.createdAt).getTime() < 7 * 24 * 3600 * 1000)
        .map((s) => s.studentId),
    );

    // 作业完成率
    const submissions = await this.submissionRepository
      .createQueryBuilder('sub')
      .where('sub.student_id IN (:...ids)', { ids: studentIds })
      .getMany();
    const gradedCount = submissions.filter((s) => s.status === 'graded' || s.status === 'submitted').length;
    const completionRate = submissions.length > 0 ? gradedCount / submissions.length : 0;

    // 各维度平均分
    const avgDim = (field: string) => {
      const vals = scoredSessions
        .map((s) => (s as any)[field]?.overall ?? (s as any)[field]?.score)
        .filter((v: any) => v != null);
      return vals.length > 0
        ? Math.round((vals.reduce((a: number, b: number) => a + b, 0) / vals.length) * 100) / 100
        : 0;
    };

    return {
      classId: cls.id,
      className: cls.name,
      studentCount: studentIds.length,
      activeStudents: recentStudents.size,
      averageScore: Math.round(avgScore * 100) / 100,
      totalSessions: sessions.length,
      assignmentCompletionRate: Math.round(completionRate * 100),
      dimensions: {
        pronunciation: avgDim('pronunciationScore'),
        fluency: avgDim('fluencyScore'),
        grammar: avgDim('grammarScore'),
        content: avgDim('contentScore'),
      },
    };
  }

  // 成绩趋势（按日期聚合各维度平均分）
  async getScoreTrends(classId: string, days = 7) {
    const cls = await this.findById(classId);
    const studentIds = cls.students.map((s) => s.id);
    if (studentIds.length === 0) return [];

    const since = new Date();
    since.setDate(since.getDate() - days);

    const sessions = await this.practiceRepository
      .createQueryBuilder('ps')
      .where('ps.student_id IN (:...ids)', { ids: studentIds })
      .andWhere('ps.created_at >= :since', { since })
      .andWhere('ps.overall_score IS NOT NULL')
      .orderBy('ps.created_at', 'ASC')
      .getMany();

    // 按日期分组
    const byDate: Record<string, any[]> = {};
    sessions.forEach((s) => {
      const date = new Date(s.createdAt).toISOString().split('T')[0];
      if (!byDate[date]) byDate[date] = [];
      byDate[date].push(s);
    });

    const dimAvg = (arr: any[], field: string) => {
      const vals = arr
        .map((s) => s[field]?.overall ?? s[field]?.score)
        .filter((v: any) => v != null);
      return vals.length > 0
        ? Math.round((vals.reduce((a: number, b: number) => a + b, 0) / vals.length) * 100) / 100
        : 0;
    };

    return Object.entries(byDate).map(([date, daySessions]) => ({
      date,
      pronunciation: dimAvg(daySessions, 'pronunciationScore'),
      fluency: dimAvg(daySessions, 'fluencyScore'),
      grammar: dimAvg(daySessions, 'grammarScore'),
      overall:
        Math.round(
          (daySessions.reduce((s, d) => s + Number(d.overallScore), 0) / daySessions.length) * 100,
        ) / 100,
    }));
  }

  // 学生排行榜（按平均分降序）
  async getStudentLeaderboard(classId: string, limit = 10) {
    const cls = await this.findById(classId);
    if (cls.students.length === 0) return [];

    const leaderboard = await Promise.all(
      cls.students.map(async (student) => {
        const sessions = await this.practiceRepository
          .createQueryBuilder('ps')
          .where('ps.student_id = :id', { id: student.id })
          .andWhere('ps.overall_score IS NOT NULL')
          .getMany();

        const avgScore =
          sessions.length > 0
            ? Math.round(
                (sessions.reduce((sum, s) => sum + Number(s.overallScore), 0) / sessions.length) *
                  100,
              ) / 100
            : 0;

        return {
          studentId: student.id,
          name: student.name,
          email: student.email,
          avgScore,
          totalSessions: sessions.length,
        };
      }),
    );

    return leaderboard
      .sort((a, b) => b.avgScore - a.avgScore)
      .slice(0, limit)
      .map((s, i) => ({ ...s, rank: i + 1 }));
  }
}
