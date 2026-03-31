import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Class } from './entities/class.entity';
import { User } from '../users/entities/user.entity';

@Injectable()
export class ClassesService {
  constructor(
    @InjectRepository(Class)
    private readonly classesRepository: Repository<Class>,
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
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

    // TODO: 查询该班级所有学生的练习数据并进行聚合分析
    return {
      classId: cls.id,
      className: cls.name,
      studentCount: cls.students.length,
      // TODO: 添加平均分、完成率、各维度评分趋势等统计数据
    };
  }
}
