import { Controller, Get, Post, Body, Req, UseGuards } from '@nestjs/common';
import { AnalyticsService } from './analytics.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { SkipThrottle } from '@nestjs/throttler';

@Controller('analytics')
export class AnalyticsController {
  constructor(private readonly analyticsService: AnalyticsService) {}

  // 埋点上报（不限流，允许批量上报）
  @Post('events')
  @SkipThrottle()
  async trackEvent(
    @Req() req: any,
    @Body() body: { eventName: string; userId?: string; payload?: Record<string, any> },
  ) {
    return this.analyticsService.track({
      eventName: body.eventName,
      userId: body.userId || req.user?.sub,
      payload: body.payload,
      ipAddress: req.ip,
      userAgent: req.headers['user-agent'],
    });
  }

  // 运营概览（需认证，仅管理员/教师）
  @Get('overview')
  @UseGuards(JwtAuthGuard)
  async getOverview() {
    return this.analyticsService.getOverview();
  }

  // DAU 趋势
  @Get('dau-trend')
  @UseGuards(JwtAuthGuard)
  async getDauTrend() {
    return this.analyticsService.getDauTrend();
  }
}
