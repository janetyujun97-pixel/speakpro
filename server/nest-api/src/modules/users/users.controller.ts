import {
  Controller,
  Get,
  Put,
  Body,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('users')
@UseGuards(JwtAuthGuard)
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('profile')
  async getProfile(@Request() req: any) {
    return this.usersService.findById(req.user.sub);
  }

  @Put('profile')
  async updateProfile(
    @Request() req: any,
    @Body() updateData: { name?: string; phone?: string; avatarUrl?: string },
  ) {
    return this.usersService.update(req.user.sub, updateData);
  }

  @Put('password')
  async changePassword(
    @Request() req: any,
    @Body() data: { currentPassword: string; newPassword: string },
  ) {
    await this.usersService.changePassword(req.user.sub, data.currentPassword, data.newPassword);
    return { message: '密码修改成功' };
  }

  @Get('settings')
  async getSettings(@Request() req: any) {
    const user = await this.usersService.findById(req.user.sub);
    return {
      asrProvider: user.asrProvider || 'tencent',
      iseProvider: user.iseProvider || 'tencent',
      llmProvider: user.llmProvider || 'mimo',
      ttsProvider: user.ttsProvider || 'mimo',
    };
  }

  @Put('settings')
  async updateSettings(
    @Request() req: any,
    @Body()
    data: {
      asrProvider?: string;
      iseProvider?: string;
      llmProvider?: string;
      ttsProvider?: string;
    },
  ) {
    const patch: Partial<{
      asrProvider: string;
      iseProvider: string;
      llmProvider: string;
      ttsProvider: string;
    }> = {};
    if (data.asrProvider) patch.asrProvider = data.asrProvider;
    if (data.iseProvider) patch.iseProvider = data.iseProvider;
    if (data.llmProvider) patch.llmProvider = data.llmProvider;
    if (data.ttsProvider) patch.ttsProvider = data.ttsProvider;
    if (Object.keys(patch).length > 0) {
      await this.usersService.update(req.user.sub, patch as any);
    }
    return { message: '设置已更新' };
  }

  @Get()
  async findByRole(@Query('role') role?: string) {
    if (role) {
      return this.usersService.findByRole(role);
    }
    return [];
  }
}
