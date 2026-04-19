import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { InternalSecretGuard } from '../../common/guards/internal-secret.guard';
import { RecordMissDto } from './dto/record-miss.dto';
import { NotebookService } from './notebook.service';

/**
 * 内部接口：Go orchestrator 评测完毕后回调。
 * 请求必须带 `X-Internal-Secret` header，与 INTERNAL_SHARED_SECRET 环境变量一致。
 * Nginx 层无额外限制；安全依赖 guard + secret。
 */
@Controller('_internal/notebook')
@UseGuards(InternalSecretGuard)
export class NotebookInternalController {
  constructor(private readonly notebookService: NotebookService) {}

  @Post('record-miss')
  async recordMiss(@Body() dto: RecordMissDto) {
    return this.notebookService.recordMiss(dto);
  }
}
