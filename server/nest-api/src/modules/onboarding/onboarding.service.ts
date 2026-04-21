import {
  BadRequestException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PracticeSession } from '../practice/entities/practice-session.entity';
import { BaselineDto } from './dto/baseline.dto';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { OnboardingProfile } from './entities/onboarding-profile.entity';

interface StudyPlan {
  weeks: number;
  dailyMinutes: number;
  focusAreas: string[];
  milestones: { week: number; goal: string }[];
}

@Injectable()
export class OnboardingService {
  private readonly logger = new Logger('OnboardingService');

  constructor(
    @InjectRepository(OnboardingProfile)
    private readonly profiles: Repository<OnboardingProfile>,
    @InjectRepository(PracticeSession)
    private readonly sessions: Repository<PracticeSession>,
  ) {}

  /** 查状态；不存在视为未开始，返回 completed:false 的空结构 */
  async getStatus(userId: string) {
    const profile = await this.profiles.findOne({ where: { userId } });
    if (!profile) {
      return {
        completed: false,
        profile: null,
      };
    }
    return {
      completed: !!profile.completedAt,
      profile,
    };
  }

  /** 增量更新（PATCH 语义）；首次访问自动 upsert 空行 */
  async updateProfile(userId: string, patch: UpdateProfileDto) {
    // target_score 跟 exam_type 联动校验
    if (patch.targetScore != null && patch.examType) {
      this.validateTargetScore(patch.examType, patch.targetScore);
    }

    const existing = await this.profiles.findOne({ where: { userId } });
    if (!existing) {
      const row = this.profiles.create({
        userId,
        examType: patch.examType ?? null,
        targetScore: patch.targetScore ?? null,
        examDate: patch.examDate ?? null,
        selfLevel: patch.selfLevel ?? null,
      });
      return this.profiles.save(row);
    }

    // 如果 target_score 未在本次传入但 examType 变了，再校验一次
    if (patch.examType && existing.targetScore != null) {
      this.validateTargetScore(patch.examType, existing.targetScore);
    }

    Object.assign(existing, {
      examType: patch.examType ?? existing.examType,
      targetScore:
        patch.targetScore != null ? patch.targetScore : existing.targetScore,
      examDate: patch.examDate ?? existing.examDate,
      selfLevel: patch.selfLevel ?? existing.selfLevel,
    });
    return this.profiles.save(existing);
  }

  /** 基线录音：优先绑定已有 practice_session，否则建一条最小记录 */
  async saveBaseline(userId: string, dto: BaselineDto) {
    if (!dto.sessionId && !dto.audioUrl) {
      throw new BadRequestException('sessionId 或 audioUrl 至少提供一个');
    }

    let sessionId = dto.sessionId;

    if (sessionId) {
      const session = await this.sessions.findOne({
        where: { id: sessionId },
      });
      if (!session) {
        throw new NotFoundException('practice_session 不存在');
      }
      if (session.studentId !== userId) {
        throw new BadRequestException('session 不属于当前用户');
      }
    } else {
      // 无 sessionId —— 用 audioUrl 建一条 mode='baseline' 的记录
      const created = this.sessions.create({
        studentId: userId,
        mode: 'baseline',
        audioUrl: dto.audioUrl,
        transcript: dto.transcript,
      });
      const saved = await this.sessions.save(created);
      sessionId = saved.id;
    }

    await this.upsertProfile(userId, { baselineSessionId: sessionId });
    return { sessionId };
  }

  /** 完成 onboarding：根据 profile 生成 study_plan，写 completed_at */
  async finalize(userId: string) {
    const profile = await this.profiles.findOne({ where: { userId } });
    if (!profile) {
      throw new BadRequestException('请先完成前面的步骤');
    }
    if (!profile.examType || profile.targetScore == null || !profile.selfLevel) {
      throw new BadRequestException('profile 尚未填完');
    }

    const plan = this.buildStudyPlan(profile);

    await this.profiles.update(
      { userId },
      {
        studyPlan: plan as any,
        completedAt: new Date(),
      },
    );

    const updated = await this.profiles.findOne({ where: { userId } });
    return { completed: true, profile: updated };
  }

  // ==================== 内部 ====================

  private async upsertProfile(
    userId: string,
    patch: Partial<OnboardingProfile>,
  ) {
    const existing = await this.profiles.findOne({ where: { userId } });
    if (!existing) {
      const created = this.profiles.create({ userId, ...patch });
      return this.profiles.save(created);
    }
    Object.assign(existing, patch);
    return this.profiles.save(existing);
  }

  private validateTargetScore(examType: string, score: number) {
    if (examType === 'IELTS') {
      if (score < 4.0 || score > 9.0) {
        throw new BadRequestException('IELTS 目标分应在 4.0 - 9.0');
      }
      // IELTS 必须 0.5 递进
      if (Math.abs(score * 2 - Math.round(score * 2)) > 1e-6) {
        throw new BadRequestException('IELTS 目标分必须是 0.5 的倍数');
      }
    } else if (examType === 'TOEFL') {
      if (score < 0 || score > 30) {
        throw new BadRequestException('TOEFL 口语目标分应在 0 - 30');
      }
    }
    // GENERAL 不强制
  }

  /**
   * 根据 exam_date + selfLevel + target_score 生成一个粗粒度学习计划。
   * 第一版用规则法（不依赖 AI），控制简单且可预测。
   * 后续可接 Qwen 做个性化；接口契约稳定后再换实现。
   */
  private buildStudyPlan(profile: OnboardingProfile): StudyPlan {
    const today = new Date();
    const exam = profile.examDate ? new Date(profile.examDate) : null;
    const daysToExam = exam
      ? Math.max(7, Math.ceil((exam.getTime() - today.getTime()) / 86400000))
      : 56;
    const weeks = Math.max(2, Math.ceil(daysToExam / 7));

    const levelMinutes: Record<number, number> = { 1: 45, 2: 40, 3: 30, 4: 25, 5: 20 };
    const dailyMinutes = levelMinutes[profile.selfLevel ?? 3] ?? 30;

    const focusByLevel: Record<number, string[]> = {
      1: ['基础发音', '常用句型', '简短对话'],
      2: ['连读弱读', '日常表达', '话题素材'],
      3: ['流利度', '词汇深度', 'Part2 结构'],
      4: ['复杂话题', '观点论证', 'Part3 辩论'],
      5: ['语言精准度', '学术语境', '考试策略'],
    };
    const focusAreas = focusByLevel[profile.selfLevel ?? 3];

    const milestones: { week: number; goal: string }[] = [];
    for (let i = 1; i <= Math.min(weeks, 8); i++) {
      const pct = Math.round((i / Math.min(weeks, 8)) * 100);
      milestones.push({ week: i, goal: `完成阶段 ${i} 训练 (${pct}%)` });
    }

    return { weeks, dailyMinutes, focusAreas, milestones };
  }
}
