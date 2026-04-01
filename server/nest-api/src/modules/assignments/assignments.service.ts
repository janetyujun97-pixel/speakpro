import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Assignment } from './entities/assignment.entity';
import { Submission } from './entities/submission.entity';

@Injectable()
export class AssignmentsService {
  constructor(
    @InjectRepository(Assignment)
    private readonly assignmentsRepository: Repository<Assignment>,
    @InjectRepository(Submission)
    private readonly submissionsRepository: Repository<Submission>,
  ) {}

  async create(data: Partial<Assignment>): Promise<Assignment> {
    const assignment = this.assignmentsRepository.create(data);
    return this.assignmentsRepository.save(assignment);
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
    data: { teacherComment?: string; teacherScore: number },
  ): Promise<Submission> {
    const submission = await this.submissionsRepository.findOne({
      where: { id: submissionId, assignmentId },
    });
    if (!submission) {
      throw new NotFoundException('提交记录不存在');
    }

    submission.teacherComment = data.teacherComment || null;
    submission.teacherScore = data.teacherScore;
    submission.status = 'graded';
    submission.gradedAt = new Date();

    return this.submissionsRepository.save(submission);
  }
}
