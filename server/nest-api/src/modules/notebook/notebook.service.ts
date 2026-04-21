import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { IsNull, LessThanOrEqual, Not, Repository } from 'typeorm';

import { NotebookPhrase } from './entities/notebook-phrase.entity';
import { NotebookWord } from './entities/notebook-word.entity';
import { RecordMissDto, RecordMissItem } from './dto/record-miss.dto';

/** ISE 单词分阈值：< 60 视为掌握不佳，纳入错题本 */
const MISS_THRESHOLD = 60;

/** SM-2 质量分映射：这里把用户点击 "想起来了" / "没想起来" 简化为 4 / 2 */
export type ReviewQuality = 0 | 1 | 2 | 3 | 4 | 5;

@Injectable()
export class NotebookService {
  constructor(
    @InjectRepository(NotebookWord)
    private readonly words: Repository<NotebookWord>,
    @InjectRepository(NotebookPhrase)
    private readonly phrases: Repository<NotebookPhrase>,
  ) {}

  // ==================== 写入（内部调用） ====================

  /** Go 服务回调：批量记录低分词 */
  async recordMiss(dto: RecordMissDto): Promise<{ recorded: number }> {
    const toInsert = dto.items.filter((it) => it.score < MISS_THRESHOLD);
    if (toInsert.length === 0) return { recorded: 0 };

    const now = new Date();
    let count = 0;
    for (const item of toInsert) {
      await this.upsertOneMiss(dto.userId, dto.sessionId, item, now);
      count++;
    }
    return { recorded: count };
  }

  private async upsertOneMiss(
    userId: string,
    sessionId: string | undefined,
    item: RecordMissItem,
    now: Date,
  ) {
    const existing = await this.words.findOne({
      where: { userId, word: item.word },
    });

    if (existing) {
      existing.missCount += 1;
      existing.lastSeenAt = now;
      existing.masteredAt = null; // 重新漏词 → 取消已掌握
      if (item.ipa && !existing.ipa) existing.ipa = item.ipa;
      if (item.note && !existing.note) existing.note = item.note;
      await this.words.save(existing);
    } else {
      await this.words.insert({
        userId,
        word: item.word,
        ipa: item.ipa ?? null,
        note: item.note ?? null,
        sourceSessionId: sessionId ?? null,
        missCount: 1,
        lastSeenAt: now,
        nextReviewAt: now, // 首次 due immediately
        ef: 2.5,
        intervalDays: 0,
      });
    }
  }

  // ==================== 查询 ====================

  async listWords(
    userId: string,
    filter: 'due' | 'mastered' | 'all' = 'all',
  ): Promise<NotebookWord[]> {
    const base = { userId };
    if (filter === 'mastered') {
      return this.words.find({
        where: { ...base, masteredAt: Not(IsNull()) },
        order: { masteredAt: 'DESC' },
      });
    }
    if (filter === 'due') {
      return this.words.find({
        where: {
          ...base,
          masteredAt: IsNull(),
          nextReviewAt: LessThanOrEqual(new Date()),
        },
        order: { nextReviewAt: 'ASC' },
      });
    }
    return this.words.find({ where: base, order: { createdAt: 'DESC' } });
  }

  async listPhrases(userId: string): Promise<NotebookPhrase[]> {
    return this.phrases.find({
      where: { userId },
      order: { createdAt: 'DESC' },
    });
  }

  // ==================== 复习 / 掌握 / 删除 ====================

  /**
   * SM-2 间隔复习（简化版，基于 SuperMemo-2）：
   *   quality ∈ [0,5]，默认调用方在客户端按钮上只给 2 / 4 两档
   *   - 若 quality < 3：interval 重置为 1 天
   *   - 否则按 SM-2 公式递推：next = interval * ef
   *   - ef 按公式 ef' = ef + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
   */
  async reviewWord(
    userId: string,
    wordId: string,
    quality: ReviewQuality,
  ): Promise<NotebookWord> {
    const row = await this.mustOwn(userId, wordId);
    const q = Math.max(0, Math.min(5, quality));

    let ef = Number(row.ef) + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
    if (ef < 1.3) ef = 1.3;

    let intervalDays: number;
    if (q < 3) {
      intervalDays = 1;
    } else if (row.intervalDays === 0) {
      intervalDays = 1;
    } else if (row.intervalDays === 1) {
      intervalDays = 6;
    } else {
      intervalDays = Math.round(row.intervalDays * ef);
    }

    row.ef = Math.round(ef * 100) / 100;
    row.intervalDays = intervalDays;
    row.lastSeenAt = new Date();
    row.nextReviewAt = new Date(Date.now() + intervalDays * 86400_000);
    if (q === 5 && intervalDays >= 30) {
      row.masteredAt = new Date();
    }
    return this.words.save(row);
  }

  async markMastered(userId: string, wordId: string): Promise<NotebookWord> {
    const row = await this.mustOwn(userId, wordId);
    row.masteredAt = new Date();
    return this.words.save(row);
  }

  async deleteWord(userId: string, wordId: string): Promise<{ ok: true }> {
    const row = await this.mustOwn(userId, wordId);
    await this.words.delete(row.id);
    return { ok: true };
  }

  private async mustOwn(userId: string, wordId: string): Promise<NotebookWord> {
    const row = await this.words.findOne({ where: { id: wordId } });
    if (!row) throw new NotFoundException('单词不存在');
    if (row.userId !== userId) {
      // 伪 404 —— 不暴露跨用户存在性
      throw new NotFoundException('单词不存在');
    }
    return row;
  }
}
