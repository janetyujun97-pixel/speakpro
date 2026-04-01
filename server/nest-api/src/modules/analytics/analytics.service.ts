import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AnalyticsEvent } from './analytics.entity';

@Injectable()
export class AnalyticsService {
  constructor(
    @InjectRepository(AnalyticsEvent)
    private readonly analyticsRepo: Repository<AnalyticsEvent>,
  ) {}

  // 记录事件
  async track(data: {
    eventName: string;
    userId?: string;
    payload?: Record<string, any>;
    ipAddress?: string;
    userAgent?: string;
  }): Promise<AnalyticsEvent> {
    const event = this.analyticsRepo.create(data);
    return this.analyticsRepo.save(event);
  }

  // 运营概览：DAU / MAU / 总用户事件数
  async getOverview() {
    const now = new Date();

    // DAU：今天有事件的唯一用户数
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const dauResult = await this.analyticsRepo
      .createQueryBuilder('e')
      .select('COUNT(DISTINCT e.user_id)', 'count')
      .where('e.created_at >= :start', { start: todayStart })
      .andWhere('e.user_id IS NOT NULL')
      .getRawOne();

    // MAU：最近 30 天有事件的唯一用户数
    const monthAgo = new Date(now.getTime() - 30 * 24 * 3600 * 1000);
    const mauResult = await this.analyticsRepo
      .createQueryBuilder('e')
      .select('COUNT(DISTINCT e.user_id)', 'count')
      .where('e.created_at >= :start', { start: monthAgo })
      .andWhere('e.user_id IS NOT NULL')
      .getRawOne();

    // 今日事件总数
    const todayEventsResult = await this.analyticsRepo
      .createQueryBuilder('e')
      .select('COUNT(*)', 'count')
      .where('e.created_at >= :start', { start: todayStart })
      .getRawOne();

    // 按事件名分组 Top 10
    const topEvents = await this.analyticsRepo
      .createQueryBuilder('e')
      .select('e.event_name', 'eventName')
      .addSelect('COUNT(*)', 'count')
      .where('e.created_at >= :start', { start: monthAgo })
      .groupBy('e.event_name')
      .orderBy('count', 'DESC')
      .limit(10)
      .getRawMany();

    return {
      dau: parseInt(dauResult?.count || '0'),
      mau: parseInt(mauResult?.count || '0'),
      todayEvents: parseInt(todayEventsResult?.count || '0'),
      topEvents,
    };
  }

  // DAU 趋势（最近 N 天）
  async getDauTrend(days = 14) {
    const since = new Date();
    since.setDate(since.getDate() - days);

    const results = await this.analyticsRepo
      .createQueryBuilder('e')
      .select("TO_CHAR(e.created_at, 'YYYY-MM-DD')", 'date')
      .addSelect('COUNT(DISTINCT e.user_id)', 'dau')
      .addSelect('COUNT(*)', 'events')
      .where('e.created_at >= :since', { since })
      .andWhere('e.user_id IS NOT NULL')
      .groupBy("TO_CHAR(e.created_at, 'YYYY-MM-DD')")
      .orderBy('date', 'ASC')
      .getRawMany();

    return results.map((r) => ({
      date: r.date,
      dau: parseInt(r.dau),
      events: parseInt(r.events),
    }));
  }
}
