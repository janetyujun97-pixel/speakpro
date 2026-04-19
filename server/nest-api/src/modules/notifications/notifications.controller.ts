import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Query,
  Request,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { UpdatePrefsDto } from './dto/update-prefs.dto';
import { NotificationsService } from './notifications.service';

@Controller()
@UseGuards(JwtAuthGuard)
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @Get('notifications')
  async list(
    @Request() req: any,
    @Query('limit') limit?: string,
  ) {
    const items = await this.notificationsService.list(
      req.user.sub,
      limit ? parseInt(limit, 10) : 50,
    );
    const unread = await this.notificationsService.unreadCount(req.user.sub);
    return { items, unread };
  }

  @Patch('notifications/read-all')
  async readAll(@Request() req: any) {
    return this.notificationsService.markAllRead(req.user.sub);
  }

  @Patch('notifications/:id/read')
  async readOne(@Request() req: any, @Param('id') id: string) {
    return this.notificationsService.markOneRead(req.user.sub, id);
  }

  // 偏好设置挂在 /users 路径下（规范与既有 /users/settings 一致）
  @Get('users/notification-prefs')
  async getPrefs(@Request() req: any) {
    return this.notificationsService.getPrefs(req.user.sub);
  }

  @Patch('users/notification-prefs')
  async updatePrefs(@Request() req: any, @Body() dto: UpdatePrefsDto) {
    return this.notificationsService.updatePrefs(req.user.sub, dto);
  }
}
