import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Query,
  Request,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { NotebookService, ReviewQuality } from './notebook.service';

@Controller('notebook')
@UseGuards(JwtAuthGuard)
export class NotebookController {
  constructor(private readonly notebookService: NotebookService) {}

  @Get('words')
  async listWords(
    @Request() req: any,
    @Query('filter') filter: 'due' | 'mastered' | 'all' = 'all',
  ) {
    return this.notebookService.listWords(req.user.sub, filter);
  }

  @Get('phrases')
  async listPhrases(@Request() req: any) {
    return this.notebookService.listPhrases(req.user.sub);
  }

  @Post('words/:id/reviewed')
  async review(
    @Request() req: any,
    @Param('id') id: string,
    @Body('quality') quality: number = 4,
  ) {
    return this.notebookService.reviewWord(
      req.user.sub,
      id,
      (Math.max(0, Math.min(5, Math.floor(quality))) as ReviewQuality),
    );
  }

  @Post('words/:id/master')
  async master(@Request() req: any, @Param('id') id: string) {
    return this.notebookService.markMastered(req.user.sub, id);
  }

  @Delete('words/:id')
  async remove(@Request() req: any, @Param('id') id: string) {
    return this.notebookService.deleteWord(req.user.sub, id);
  }
}
