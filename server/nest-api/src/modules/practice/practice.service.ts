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
    const sessions = await this.practiceRepository.find({
      where: { studentId },
      relations: ['question'],
    });

    const totalSessions = sessions.length;
    const scoredSessions = sessions.filter((s) => s.overallScore != null);
    const avgScore =
      scoredSessions.reduce((sum, s) => sum + Number(s.overallScore), 0) /
      (scoredSessions.length || 1);
    const totalDuration = sessions.reduce((sum, s) => sum + (s.durationSec || 0), 0);

    // 按练习模式分组
    const byMode: Record<string, number> = {};
    sessions.forEach((s) => {
      byMode[s.mode] = (byMode[s.mode] || 0) + 1;
    });

    // 按考试类型分组
    const byExamType: Record<string, number> = {};
    sessions.forEach((s) => {
      const examType = s.question?.examType || 'unknown';
      byExamType[examType] = (byExamType[examType] || 0) + 1;
    });

    // 各维度平均分
    const avgPronunciation = this.avgJsonScore(scoredSessions, 'pronunciationScore');
    const avgFluency = this.avgJsonScore(scoredSessions, 'fluencyScore');
    const avgGrammar = this.avgJsonScore(scoredSessions, 'grammarScore');
    const avgContent = this.avgJsonScore(scoredSessions, 'contentScore');

    // 最近 7 天和 30 天的练习次数
    const now = new Date();
    const last7Days = sessions.filter(
      (s) => now.getTime() - new Date(s.createdAt).getTime() < 7 * 24 * 3600 * 1000,
    ).length;
    const last30Days = sessions.filter(
      (s) => now.getTime() - new Date(s.createdAt).getTime() < 30 * 24 * 3600 * 1000,
    ).length;

    return {
      totalSessions,
      averageScore: Math.round(avgScore * 100) / 100,
      totalDurationMin: Math.round(totalDuration / 60),
      byMode,
      byExamType,
      dimensions: {
        pronunciation: avgPronunciation,
        fluency: avgFluency,
        grammar: avgGrammar,
        content: avgContent,
      },
      recent: { last7Days, last30Days },
    };
  }

  private avgJsonScore(
    sessions: PracticeSession[],
    field: 'pronunciationScore' | 'fluencyScore' | 'grammarScore' | 'contentScore',
  ): number {
    const scored = sessions.filter((s) => s[field]?.overall != null || s[field]?.score != null);
    if (scored.length === 0) return 0;
    const sum = scored.reduce((acc, s) => acc + (s[field]?.overall ?? s[field]?.score ?? 0), 0);
    return Math.round((sum / scored.length) * 100) / 100;
  }
}
