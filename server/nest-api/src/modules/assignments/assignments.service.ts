import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Class } from '../classes/entities/class.entity';
import { NotificationsService } from '../notifications/notifications.service';
import { Assignment } from './entities/assignment.entity';
import { Submission } from './entities/submission.entity';

@Injectable()
export class AssignmentsService {
  constructor(
    @InjectRepository(Assignment)
    private readonly assignmentsRepository: Repository<Assignment>,
    @InjectRepository(Submission)
    private readonly submissionsRepository: Repository<Submission>,
    @InjectRepository(Class)
    private readonly classesRepository: Repository<Class>,
    private readonly notificationsService: NotificationsService,
  ) {}

  async create(data: Partial<Assignment>): Promise<Assignment> {
    const assignment = this.assignmentsRepository.create(data);
    const saved = await this.assignmentsRepository.save(assignment);
    await this.fanOutHomeworkNotifications(saved);
    return saved;
  }

  /**
   * 作业创建后 → 为班级每个学生写一条 homework 通知。
   * 失败不阻塞作业保存（e.g. 班级为空 / 查询失败）。
   */
  private async fanOutHomeworkNotifications(assignment: Assignment) {
    if (!assignment.classId) return;
    try {
      const cls = await this.classesRepository.findOne({
        where: { id: assignment.classId },
        relations: ['students'],
      });
      if (!cls || !cls.students?.length) return;

      await this.notificationsService.createMany(
        cls.students.map((s) => ({
          userId: s.id,
          kind: 'homework' as const,
          title: '新作业：' + assignment.title,
          body: assignment.description ?? '老师布置了一份新作业',
          payload: { assignmentId: assignment.id, classId: assignment.classId },
        })),
      );
    } catch {
      // 容错：通知不影响主流程
    }
  }

  async findAll(filters?: { classId?: string; teacherId?: string }): Promise<Assignment[]> {
    const query = this.assignmentsRepository.createQueryBuilder('a')
      .leftJoinAndSelect('a.submissions', 'submissions')
      .leftJoinAndSelect('a.teacher', 'teacher');
    if (filters?.classId) {
      query.andWhere('a.class_id = :classId', { classId: filters.classId });
    }
    if (filters?.teacherId) {
      query.andWhere('a.teacher_id = :teacherId', { teacherId: filters.teacherId });
    }
    query.orderBy('a.created_at', 'DESC');
    return query.getMany();
  }

  async getSubmissionDetail(assignmentId: string, submissionId: string) {
    const submission = await this.submissionsRepository.findOne({
      where: { id: submissionId, assignmentId },
    });
    if (!submission) {
      throw new NotFoundException('提交记录不存在');
    }
    return submission;
  }

  async findById(id: string): Promise<Assignment> {
    const assignment = await this.assignmentsRepository.findOne({
      where: { id },
      relations: ['submissions', 'teacher'],
    });
    if (!assignment) {
      throw new NotFoundException('作业不存在');
    }
    return assignment;
  }

  async submit(
    assignmentId: string,
    studentId: string,
    data: { sessionIds: string[] },
  ): Promise<Submission> {
    // 检查作业是否存在
    await this.findById(assignmentId);

    // 查找或创建提交记录
    let submission = await this.submissionsRepository.findOne({
      where: { assignmentId, studentId },
    });

    if (submission) {
      submission.sessionIds = data.sessionIds;
      submission.status = 'submitted';
      submission.submittedAt = new Date();
    } else {
      submission = this.submissionsRepository.create({
        assignmentId,
        studentId,
        sessionIds: data.sessionIds,
        status: 'submitted',
        submittedAt: new Date(),
      });
    }

    return this.submissionsRepository.save(submission);
  }

  async grade(
    assignmentId: string,
    submissionId: string,
    data: {
      teacherComment?: string;
      teacherScore: number;
      teacherVoiceUrl?: string;
    },
  ): Promise<Submission> {
    const submission = await this.submissionsRepository.findOne({
      where: { id: submissionId, assignmentId },
    });
    if (!submission) {
      throw new NotFoundException('提交记录不存在');
    }

    submission.teacherComment = data.teacherComment || null;
    submission.teacherScore = data.teacherScore;
    submission.teacherVoiceUrl = data.teacherVoiceUrl ?? null;
    submission.status = 'graded';
    submission.gradedAt = new Date();

    const saved = await this.submissionsRepository.save(submission);

    // 批改完成 → 写一条 feedback 通知给学生
    try {
      const assignment = await this.assignmentsRepository.findOne({
        where: { id: assignmentId },
      });
      if (assignment) {
        await this.notificationsService.create({
          userId: submission.studentId,
          kind: 'feedback',
          title: '老师已批改：' + assignment.title,
          body: data.teacherComment || `得分 ${data.teacherScore}`,
          payload: { assignmentId, submissionId, score: data.teacherScore },
        });
      }
    } catch {
      // 容错
    }

    return saved;
  }

  /** Web 教师后台录完语音 → PATCH assignment 级别的语音备注 */
  async updateTeacherVoice(
    assignmentId: string,
    teacherVoiceUrl: string | null,
  ): Promise<Assignment> {
    const assignment = await this.findById(assignmentId);
    assignment.teacherVoiceUrl = teacherVoiceUrl;
    return this.assignmentsRepository.save(assignment);
  }
}
