import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException } from '@nestjs/common';
import { NotificationsService } from './notifications.service';
import { Notification } from './entities/notification.entity';
import { UserNotificationPrefs } from './entities/user-notification-prefs.entity';

describe('NotificationsService', () => {
  let service: NotificationsService;
  let repo: {
    create: jest.Mock;
    save: jest.Mock;
    find: jest.Mock;
    findOne: jest.Mock;
    count: jest.Mock;
    update: jest.Mock;
  };
  let prefs: {
    findOne: jest.Mock;
    create: jest.Mock;
    save: jest.Mock;
  };

  beforeEach(async () => {
    repo = {
      create: jest.fn((v) => v),
      save: jest.fn((v) => Promise.resolve(Array.isArray(v) ? v : { ...v, id: 'n1' })),
      find: jest.fn(),
      findOne: jest.fn(),
      count: jest.fn(),
      update: jest.fn().mockResolvedValue({ affected: 3 }),
    };
    prefs = {
      findOne: jest.fn(),
      create: jest.fn((v) => v),
      save: jest.fn((v) => Promise.resolve(v)),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        NotificationsService,
        { provide: getRepositoryToken(Notification), useValue: repo },
        { provide: getRepositoryToken(UserNotificationPrefs), useValue: prefs },
      ],
    }).compile();

    service = module.get(NotificationsService);
  });

  it('create 生成单条通知', async () => {
    const r: any = await service.create({
      userId: 'u1', kind: 'homework', title: 't', body: 'b',
    });
    expect(r.title).toBe('t');
    expect(repo.save).toHaveBeenCalled();
  });

  it('createMany 空数组返回 0 且不调 save', async () => {
    const n = await service.createMany([]);
    expect(n).toBe(0);
    expect(repo.save).not.toHaveBeenCalled();
  });

  it('createMany 批量保存', async () => {
    await service.createMany([
      { userId: 'u1', kind: 'homework', title: 't1', body: 'b1' },
      { userId: 'u2', kind: 'homework', title: 't2', body: 'b2' },
    ]);
    expect(repo.save).toHaveBeenCalled();
  });

  it('markAllRead 返回 affected 数', async () => {
    const res = await service.markAllRead('u1');
    expect(res.updated).toBe(3);
  });

  it('markOneRead 非本人通知抛 NotFound', async () => {
    repo.findOne.mockResolvedValue({ id: 'n1', userId: 'other', isRead: false });
    await expect(service.markOneRead('u1', 'n1')).rejects.toThrow(NotFoundException);
  });

  it('markOneRead 正常通知置 isRead=true', async () => {
    repo.findOne.mockResolvedValue({ id: 'n1', userId: 'u1', isRead: false });
    await service.markOneRead('u1', 'n1');
    expect(repo.save).toHaveBeenCalledWith(expect.objectContaining({ isRead: true }));
  });

  it('getPrefs 首次自动建默认记录', async () => {
    prefs.findOne.mockResolvedValue(null);
    await service.getPrefs('u1');
    expect(prefs.create).toHaveBeenCalledWith({ userId: 'u1' });
    expect(prefs.save).toHaveBeenCalled();
  });

  it('updatePrefs HH:MM 规范化为 HH:MM:SS', async () => {
    prefs.findOne.mockResolvedValue({ userId: 'u1', quietStart: '22:30:00' });
    const res: any = await service.updatePrefs('u1', { quietStart: '21:00' });
    expect(res.quietStart).toBe('21:00:00');
  });
});
