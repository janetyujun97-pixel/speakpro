import {
  Injectable,
  NotFoundException,
  BadRequestException,
  ConflictException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { User } from './entities/user.entity';

// 用户列表查询参数
export interface ListUsersParams {
  role?: string;
  status?: string;
  q?: string;
  page?: number;
  pageSize?: number;
}

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
  ) {}

  async findById(id: string): Promise<User> {
    const user = await this.usersRepository.findOne({ where: { id } });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    return user;
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.usersRepository.findOne({
      where: { email },
      select: ['id', 'email', 'password', 'name', 'role', 'status', 'phone', 'avatarUrl', 'createdAt', 'updatedAt'],
    });
  }

  async findByPhone(phone: string): Promise<User | null> {
    return this.usersRepository.findOne({ where: { phone } });
  }

  async findByAppleSub(appleSub: string): Promise<User | null> {
    return this.usersRepository.findOne({ where: { appleSub } });
  }

  async findByWechatUnionid(wechatUnionid: string): Promise<User | null> {
    return this.usersRepository.findOne({ where: { wechatUnionid } });
  }

  async setPassword(userId: string, newPassword: string): Promise<void> {
    const hashed = await bcrypt.hash(newPassword, 10);
    await this.usersRepository.update(userId, { password: hashed });
  }

  async create(data: Partial<User>): Promise<User> {
    const user = this.usersRepository.create(data);
    return this.usersRepository.save(user);
  }

  async update(id: string, data: Partial<User>): Promise<User> {
    await this.usersRepository.update(id, data);
    return this.findById(id);
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string): Promise<void> {
    const user = await this.usersRepository.findOne({
      where: { id: userId },
      select: ['id', 'password'],
    });
    if (!user) throw new NotFoundException('用户不存在');

    const isMatch = await bcrypt.compare(currentPassword, user.password);
    if (!isMatch) throw new BadRequestException('当前密码不正确');

    const hashedPassword = await bcrypt.hash(newPassword, 10);
    await this.usersRepository.update(userId, { password: hashedPassword });
  }

  async findByRole(role: string): Promise<User[]> {
    return this.usersRepository.find({
      where: { role: role as any },
      select: ['id', 'name', 'email', 'role'],
      order: { name: 'ASC' },
    });
  }

  // ==================== 管理员：用户管理 ====================

  /**
   * 分页列出用户。teacher 调用时强制只看学生（用于加入班级时检索）；admin 不限。
   * 教师列表附带其拥有的班级数 classCount。
   */
  async listUsers(params: ListUsersParams, requesterRole: string) {
    const page = Math.max(1, Number(params.page) || 1);
    const pageSize = Math.min(100, Math.max(1, Number(params.pageSize) || 20));

    // 教师只能检索学生
    const role = requesterRole === 'teacher' ? 'student' : params.role;

    const qb = this.usersRepository
      .createQueryBuilder('u')
      .select(['u.id', 'u.name', 'u.email', 'u.phone', 'u.role', 'u.status', 'u.createdAt'])
      .orderBy('u.createdAt', 'DESC')
      .skip((page - 1) * pageSize)
      .take(pageSize);

    if (role) qb.andWhere('u.role = :role', { role });
    if (params.status) qb.andWhere('u.status = :status', { status: params.status });
    if (params.q) {
      qb.andWhere('(u.name ILIKE :q OR u.email ILIKE :q)', { q: `%${params.q}%` });
    }

    const [items, total] = await qb.getManyAndCount();

    // 教师列表附带班级数（classes.teacher_id 关联）
    let enriched: any[] = items;
    if ((role === 'teacher' || !role) && items.length > 0) {
      const teacherIds = items.filter((u) => u.role === 'teacher').map((u) => u.id);
      if (teacherIds.length > 0) {
        const rows: Array<{ teacher_id: string; cnt: string }> =
          await this.usersRepository.manager.query(
            'SELECT teacher_id, COUNT(*)::text AS cnt FROM classes WHERE teacher_id = ANY($1) GROUP BY teacher_id',
            [teacherIds],
          );
        const countMap = new Map(rows.map((r) => [r.teacher_id, Number(r.cnt)]));
        enriched = items.map((u) =>
          u.role === 'teacher' ? { ...u, classCount: countMap.get(u.id) || 0 } : u,
        );
      }
    }

    return { items: enriched, total, page, pageSize };
  }

  /** 管理员创建账号 */
  async createUser(data: {
    name: string;
    email: string;
    password: string;
    role: 'student' | 'teacher' | 'admin';
  }): Promise<User> {
    const existing = await this.usersRepository.findOne({ where: { email: data.email } });
    if (existing) {
      throw new ConflictException('该邮箱已注册');
    }
    const hashed = await bcrypt.hash(data.password, 10);
    const user = this.usersRepository.create({
      name: data.name,
      email: data.email,
      password: hashed,
      role: data.role,
      status: 'active',
    });
    const saved = await this.usersRepository.save(user);
    return this.findById(saved.id);
  }

  /** 管理员编辑账号基本信息（name/email/role） */
  async updateUser(
    id: string,
    data: { name?: string; email?: string; role?: 'student' | 'teacher' | 'admin' },
  ): Promise<User> {
    await this.findById(id); // 不存在则抛 404
    if (data.email) {
      const other = await this.usersRepository.findOne({ where: { email: data.email } });
      if (other && other.id !== id) {
        throw new ConflictException('该邮箱已被其他账号使用');
      }
    }
    const patch: Partial<User> = {};
    if (data.name !== undefined) patch.name = data.name;
    if (data.email !== undefined) patch.email = data.email;
    if (data.role !== undefined) patch.role = data.role;
    if (Object.keys(patch).length > 0) {
      await this.usersRepository.update(id, patch);
    }
    return this.findById(id);
  }

  /** 启用/禁用账号 */
  async updateStatus(id: string, status: 'active' | 'disabled'): Promise<User> {
    await this.findById(id);
    await this.usersRepository.update(id, { status });
    return this.findById(id);
  }

  /** 管理员重置密码 */
  async resetPassword(id: string, newPassword: string): Promise<void> {
    await this.findById(id);
    const hashed = await bcrypt.hash(newPassword, 10);
    await this.usersRepository.update(id, { password: hashed });
  }
}
