import {
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';

import { Notification, NotificationKind } from './entities/notification.entity';
import { UserNotificationPrefs } from './entities/user-notification-prefs.entity';
import { UpdatePrefsDto } from './dto/update-prefs.dto';

interface CreateOne {
  userId: string;
  kind: NotificationKind;
  title: string;
  body: string;
  payload?: Record<string, any>;
}

@Injectable()
export class NotificationsService {
  constructor(
    @InjectRepository(Notification)
    private readonly notifications: Repository<Notification>,
    @InjectRepository(UserNotificationPrefs)
    private readonly prefs: Repository<UserNotificationPrefs>,
  ) {}

  // ==================== 创建（供其他模块调用） ====================

  async create(item: CreateOne): Promise<Notification> {
    const row = this.notifications.create({
      userId: item.userId,
      kind: item.kind,
      title: item.title,
      body: item.body,
      payload: item.payload ?? null,
    });
    return this.notifications.save(row);
  }

  /** 批量创建（作业通知 fan-out 用） */
  async createMany(items: CreateOne[]): Promise<number> {
    if (items.length === 0) return 0;
    const rows = items.map((it) =>
      this.notifications.create({
        userId: it.userId,
        kind: it.kind,
        title: it.title,
        body: it.body,
        payload: it.payload ?? null,
      }),
    );
    await this.notifications.save(rows);
    return rows.length;
  }

  // ==================== 查询 ====================

  async list(userId: string, limit = 50): Promise<Notification[]> {
    return this.notifications.find({
      where: { userId },
      order: { createdAt: 'DESC' },
      take: Math.min(200, Math.max(1, limit)),
    });
  }

  async unreadCount(userId: string): Promise<number> {
    return this.notifications.count({ where: { userId, isRead: false } });
  }

  // ==================== 已读 ====================

  async markOneRead(userId: string, id: string) {
    const row = await this.notifications.findOne({ where: { id } });
    if (!row) throw new NotFoundException('通知不存在');
    if (row.userId !== userId) throw new NotFoundException('通知不存在');
    if (!row.isRead) {
      row.isRead = true;
      await this.notifications.save(row);
    }
    return row;
  }

  async markAllRead(userId: string): Promise<{ updated: number }> {
    const res = await this.notifications.update(
      { userId, isRead: false },
      { isRead: true },
    );
    return { updated: res.affected ?? 0 };
  }

  // ==================== 偏好 ====================

  async getPrefs(userId: string): Promise<UserNotificationPrefs> {
    let row = await this.prefs.findOne({ where: { userId } });
    if (!row) {
      row = this.prefs.create({ userId });
      await this.prefs.save(row);
    }
    return row;
  }

  async updatePrefs(
    userId: string,
    patch: UpdatePrefsDto,
  ): Promise<UserNotificationPrefs> {
    const row = await this.getPrefs(userId);
    if (patch.quietStart !== undefined) row.quietStart = normalize(patch.quietStart);
    if (patch.quietEnd !== undefined) row.quietEnd = normalize(patch.quietEnd);
    if (patch.pushEnabled !== undefined) row.pushEnabled = patch.pushEnabled;
    return this.prefs.save(row);
  }
}

function normalize(t: string): string {
  // Postgres TIME 存储到秒粒度，接受 HH:MM 或 HH:MM:SS
  return t.length === 5 ? `${t}:00` : t;
}
