import {
  Controller,
  Get,
  Post,
  Patch,
  Body,
  Param,
  UseGuards,
  Request,
} from '@nestjs/common';
import { PracticeService } from './practice.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('practice')
export class PracticeController {
  constructor(private readonly practiceService: PracticeService) {}

  @Post('start')
  @UseGuards(JwtAuthGuard)
  async startSession(
    @Request() req: any,
    @Body() data: { questionId: string; mode: string },
  ) {
    return this.practiceService.startSession(req.user.sub, data);
  }

  @Post('audio')
  @UseGuards(JwtAuthGuard)
  async uploadAudio(
    @Body() data: { sessionId: string; audioUrl: string; durationSec?: number },
  ) {
    return this.practiceService.uploadAudio(data.sessionId, {
      audioUrl: data.audioUrl,
      durationSec: data.durationSec,
    });
  }

  @Get('sessions')
  @UseGuards(JwtAuthGuard)
  async findSessions(@Request() req: any) {
    return this.practiceService.findSessions(req.user.sub);
  }

  @Get('sessions/:id')
  @UseGuards(JwtAuthGuard)
  async findById(@Param('id') id: string) {
    return this.practiceService.findById(id);
  }

  @Get('stats')
  @UseGuards(JwtAuthGuard)
  async getStats(@Request() req: any) {
    return this.practiceService.getStats(req.user.sub);
  }

  // 批量查询 sessions（用于批改页加载练习详情）
  @Post('sessions/batch')
  @UseGuards(JwtAuthGuard)
  async findBySessionIds(@Body() body: { sessionIds: string[] }) {
    return this.practiceService.findBySessionIds(body.sessionIds);
  }

  // Go 服务内部回调接口 —— 回写评测结果（无需 JWT 认证）
  @Patch('sessions/:id/scores')
  async updateScores(
    @Param('id') id: string,
    @Body()
    scores: {
      pronunciationScore?: Record<string, any>;
      fluencyScore?: Record<string, any>;
      grammarScore?: Record<string, any>;
      contentScore?: Record<string, any>;
      overallScore?: number;
      aiFeedback?: string;
      transcript?: string;
    },
  ) {
    return this.practiceService.updateScores(id, scores);
  }
}
