import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PracticeSession } from './entities/practice-session.entity';

@Injectable()
export class PracticeService {
  constructor(
    @InjectRepository(PracticeSession)
    private readonly practiceRepository: Repository<PracticeSession>,
  ) {}

  async startSession(studentId: string, data: { questionId: string; mode: string }): Promise<PracticeSession> {
    const session = this.practiceRepository.create({
      studentId,
      questionId: data.questionId,
      mode: data.mode,
    });
    return this.practiceRepository.save(session);
  }

  async uploadAudio(
    sessionId: string,
    audioData: { audioUrl: string; durationSec?: number },
  ): Promise<PracticeSession> {
    const session = await this.findById(sessionId);
    session.audioUrl = audioData.audioUrl;
    if (audioData.durationSec) {
      session.durationSec = audioData.durationSec;
    }
    // TODO: 触发 AI 评分与反馈流程
    return this.practiceRepository.save(session);
  }

  async findSessions(studentId: string): Promise<PracticeSession[]> {
    return this.practiceRepository.find({
      where: { studentId },
      order: { createdAt: 'DESC' },
      relations: ['question'],
    });
  }

  async findById(id: string): Promise<PracticeSession> {
    const session = await this.practiceRepository.findOne({
      where: { id },
      relations: ['question'],
    });
    if (!session) {
      throw new NotFoundException('练习会话不存在');
    }
    return session;
  }

  async getStats(studentId: string) {
    // TODO: 实现详细的统计分析逻辑（按考试类型、题目分区、时间维度等聚合）
    const sessions = await this.practiceRepository.find({
      where: { studentId },
    });

    const totalSessions = sessions.length;
    const avgScore =
      sessions.reduce((sum, s) => sum + (Number(s.overallScore) || 0), 0) /
      (totalSessions || 1);

    return {
      totalSessions,
      averageScore: Math.round(avgScore * 100) / 100,
      // TODO: 添加更多维度的统计数据
    };
  }
}
