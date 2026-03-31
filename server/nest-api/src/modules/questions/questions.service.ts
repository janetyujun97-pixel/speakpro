import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Question } from './entities/question.entity';

export interface QuestionFilters {
  examType?: string;
  section?: string;
  difficulty?: number;
}

@Injectable()
export class QuestionsService {
  constructor(
    @InjectRepository(Question)
    private readonly questionsRepository: Repository<Question>,
  ) {}

  async findAll(filters: QuestionFilters): Promise<Question[]> {
    const query = this.questionsRepository.createQueryBuilder('q');

    if (filters.examType) {
      query.andWhere('q.exam_type = :examType', { examType: filters.examType });
    }
    if (filters.section) {
      query.andWhere('q.section = :section', { section: filters.section });
    }
    if (filters.difficulty) {
      query.andWhere('q.difficulty = :difficulty', { difficulty: filters.difficulty });
    }

    query.orderBy('q.created_at', 'DESC');
    return query.getMany();
  }

  async findById(id: string): Promise<Question> {
    const question = await this.questionsRepository.findOne({ where: { id } });
    if (!question) {
      throw new NotFoundException('题目不存在');
    }
    return question;
  }

  async create(data: Partial<Question>): Promise<Question> {
    const question = this.questionsRepository.create(data);
    return this.questionsRepository.save(question);
  }

  async update(id: string, data: Partial<Question>): Promise<Question> {
    await this.findById(id); // 确保题目存在
    await this.questionsRepository.update(id, data);
    return this.findById(id);
  }

  async delete(id: string): Promise<void> {
    await this.findById(id); // 确保题目存在
    await this.questionsRepository.delete(id);
  }

  async bulkImport(questions: Partial<Question>[]): Promise<{
    success: number;
    failed: number;
    errors: string[];
    data: Question[];
  }> {
    const errors: string[] = [];
    const validQuestions: Partial<Question>[] = [];

    // 逐条校验必填字段
    questions.forEach((q, i) => {
      const missing: string[] = [];
      if (!q.examType) missing.push('exam_type');
      if (!q.section) missing.push('section');
      if (!q.promptText) missing.push('prompt_text');
      if (missing.length > 0) {
        errors.push(`第 ${i + 1} 条: 缺少必填字段 ${missing.join(', ')}`);
      } else {
        validQuestions.push(q);
      }
    });

    // 使用事务批量插入有效数据
    let saved: Question[] = [];
    if (validQuestions.length > 0) {
      const queryRunner = this.questionsRepository.manager.connection.createQueryRunner();
      await queryRunner.connect();
      await queryRunner.startTransaction();
      try {
        const entities = this.questionsRepository.create(validQuestions);
        saved = await queryRunner.manager.save(entities);
        await queryRunner.commitTransaction();
      } catch (err) {
        await queryRunner.rollbackTransaction();
        errors.push(`批量插入失败: ${(err as Error).message}`);
      } finally {
        await queryRunner.release();
      }
    }

    return {
      success: saved.length,
      failed: questions.length - saved.length,
      errors,
      data: saved,
    };
  }
}
