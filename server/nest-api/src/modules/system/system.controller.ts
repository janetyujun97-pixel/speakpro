import {
  Controller,
  Get,
  UseGuards,
  InternalServerErrorException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';

// 代理 Go 服务健康检查，仅 admin 可访问
@Controller('system')
@UseGuards(JwtAuthGuard, RolesGuard)
export class SystemController {
  constructor(private readonly config: ConfigService) {}

  @Get('ai-health')
  @Roles('admin')
  async aiHealth() {
    const base = this.config.get<string>('GO_SERVICE_URL', 'http://localhost:8080');
    const url = `${base.replace(/\/$/, '')}/api/v1/health`;
    try {
      const res = await fetch(url, { signal: AbortSignal.timeout(3000) });
      if (!res.ok) {
        return { status: 'down', ai: null, error: `Go 服务返回 ${res.status}` };
      }
      const data = await res.json();
      return { status: data.status || 'ok', ai: data.ai || null };
    } catch (err: any) {
      // 健康检查失败 → 返回降级状态而非抛错，让前端显示红灯
      return { status: 'down', ai: null, error: err?.message || '无法连接 Go 服务' };
    }
  }
}
