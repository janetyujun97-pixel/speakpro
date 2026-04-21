import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException } from '@nestjs/common';
import { NotebookService } from './notebook.service';
import { NotebookWord } from './entities/notebook-word.entity';
import { NotebookPhrase } from './entities/notebook-phrase.entity';

describe('NotebookService', () => {
  let service: NotebookService;
  let words: {
    findOne: jest.Mock;
    insert: jest.Mock;
    save: jest.Mock;
    find: jest.Mock;
    delete: jest.Mock;
  };
  let phrases: { find: jest.Mock };

  beforeEach(async () => {
    words = {
      findOne: jest.fn(),
      insert: jest.fn(),
      save: jest.fn((v) => Promise.resolve(v)),
      find: jest.fn(),
      delete: jest.fn(),
    };
    phrases = { find: jest.fn().mockResolvedValue([]) };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        NotebookService,
        { provide: getRepositoryToken(NotebookWord), useValue: words },
        { provide: getRepositoryToken(NotebookPhrase), useValue: phrases },
      ],
    }).compile();

    service = module.get<NotebookService>(NotebookService);
  });

  describe('recordMiss', () => {
    it('仅写入 score < 60 的词', async () => {
      words.findOne.mockResolvedValue(null);
      const res = await service.recordMiss({
        userId: 'u1',
        items: [
          { word: 'apple', score: 55 },
          { word: 'banana', score: 80 }, // 过滤掉
          { word: 'cherry', score: 30 },
        ],
      });
      expect(res.recorded).toBe(2);
      expect(words.insert).toHaveBeenCalledTimes(2);
    });

    it('已存在的词 missCount+1、清 masteredAt', async () => {
      words.findOne.mockResolvedValue({
        id: 'w1', userId: 'u1', word: 'apple',
        missCount: 3, masteredAt: new Date(), ipa: null, note: null,
      });
      await service.recordMiss({
        userId: 'u1',
        items: [{ word: 'apple', score: 40 }],
      });
      expect(words.save).toHaveBeenCalledWith(expect.objectContaining({
        missCount: 4,
        masteredAt: null,
      }));
      expect(words.insert).not.toHaveBeenCalled();
    });

    it('空 items 不做任何写入', async () => {
      const res = await service.recordMiss({ userId: 'u1', items: [] });
      expect(res.recorded).toBe(0);
      expect(words.insert).not.toHaveBeenCalled();
    });
  });

  describe('reviewWord (SM-2)', () => {
    const base = () => ({
      id: 'w1', userId: 'u1', word: 'apple',
      missCount: 1, lastSeenAt: null, masteredAt: null,
      nextReviewAt: new Date(), ef: 2.5, intervalDays: 0,
      sourceSessionId: null, ipa: null, note: null,
      createdAt: new Date(),
    });

    beforeEach(() => {
      words.findOne.mockImplementation(() => Promise.resolve(base()));
      words.save.mockImplementation((v) => Promise.resolve(v));
    });

    it('quality < 3 重置 interval 为 1 天', async () => {
      const res: any = await service.reviewWord('u1', 'w1', 1);
      expect(res.intervalDays).toBe(1);
    });

    it('首次 quality=4 interval 为 1 天，ef 上调', async () => {
      const res: any = await service.reviewWord('u1', 'w1', 4);
      expect(res.intervalDays).toBe(1);
      expect(res.ef).toBeGreaterThanOrEqual(2.5);
    });

    it('二次 quality=5 从 1 天跳到 6 天', async () => {
      words.findOne.mockResolvedValue({ ...base(), intervalDays: 1, ef: 2.6 });
      const res: any = await service.reviewWord('u1', 'w1', 5);
      expect(res.intervalDays).toBe(6);
    });

    it('三次及以后按 interval * ef 递推', async () => {
      words.findOne.mockResolvedValue({ ...base(), intervalDays: 6, ef: 2.6 });
      const res: any = await service.reviewWord('u1', 'w1', 4);
      // 6 * 2.6 ≈ 16
      expect(res.intervalDays).toBeGreaterThanOrEqual(14);
      expect(res.intervalDays).toBeLessThanOrEqual(18);
    });

    it('非本人的单词抛 NotFound', async () => {
      words.findOne.mockResolvedValue({ ...base(), userId: 'other' });
      await expect(
        service.reviewWord('u1', 'w1', 4),
      ).rejects.toThrow(NotFoundException);
    });
  });

  describe('markMastered / deleteWord', () => {
    it('markMastered 写入 masteredAt', async () => {
      words.findOne.mockResolvedValue({
        id: 'w1', userId: 'u1', masteredAt: null,
      });
      const res: any = await service.markMastered('u1', 'w1');
      expect(res.masteredAt).toBeInstanceOf(Date);
    });

    it('deleteWord 删除并返回 ok', async () => {
      words.findOne.mockResolvedValue({ id: 'w1', userId: 'u1' });
      const res = await service.deleteWord('u1', 'w1');
      expect(res.ok).toBe(true);
      expect(words.delete).toHaveBeenCalledWith('w1');
    });
  });
});
