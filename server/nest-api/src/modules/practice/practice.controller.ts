import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  UseGuards,
  Request,
} from '@nestjs/common';
import { PracticeService } from './practice.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('practice')
@UseGuards(JwtAuthGuard)
export class PracticeController {
  constructor(private readonly practiceService: PracticeService) {}

  @Post('start')
  async startSession(
    @Request() req,
    @Body() data: { questionId: string; mode: string },
  ) {
    return this.practiceService.startSession(req.user.sub, data);
  }

  @Post('audio')
  async uploadAudio(
    @Body() data: { sessionId: string; audioUrl: string; durationSec?: number },
  ) {
    return this.practiceService.uploadAudio(data.sessionId, {
      audioUrl: data.audioUrl,
      durationSec: data.durationSec,
    });
  }

  @Get('sessions')
  async findSessions(@Request() req) {
    return this.practiceService.findSessions(req.user.sub);
  }

  @Get('sessions/:id')
  async findById(@Param('id') id: string) {
    return this.practiceService.findById(id);
  }

  @Get('stats')
  async getStats(@Request() req) {
    return this.practiceService.getStats(req.user.sub);
  }
}
