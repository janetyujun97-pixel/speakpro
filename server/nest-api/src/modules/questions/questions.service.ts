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

  async bulkImport(questions: Partial<Question>[]): Promise<Question[]> {
    // TODO: 添加批量导入验证逻辑
    const entities = this.questionsRepository.create(questions);
    return this.questionsRepository.save(entities);
  }
}
