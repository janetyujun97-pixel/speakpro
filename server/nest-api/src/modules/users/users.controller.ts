import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CreateUserDto } from './dto/create-user.dto';
import {
  UpdateUserDto,
  UpdateStatusDto,
  ResetPasswordDto,
} from './dto/update-user.dto';

@Controller('users')
@UseGuards(JwtAuthGuard, RolesGuard)
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  // ==================== 个人（任意已登录角色） ====================

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

  // ==================== 用户管理 ====================

  // 列表：teacher 仅能检索学生（service 内强制），admin 可按 role/status/q 检索全部
  @Get()
  @Roles('teacher', 'admin')
  async list(
    @Request() req: any,
    @Query('role') role?: string,
    @Query('status') status?: string,
    @Query('q') q?: string,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return this.usersService.listUsers(
      {
        role,
        status,
        q,
        page: page ? Number(page) : undefined,
        pageSize: pageSize ? Number(pageSize) : undefined,
      },
      req.user.role,
    );
  }

  @Get(':id')
  @Roles('admin')
  async getById(@Param('id') id: string) {
    return this.usersService.findById(id);
  }

  @Post()
  @Roles('admin')
  async create(@Body() dto: CreateUserDto) {
    return this.usersService.createUser(dto);
  }

  @Put(':id')
  @Roles('admin')
  async update(@Param('id') id: string, @Body() dto: UpdateUserDto) {
    return this.usersService.updateUser(id, dto);
  }

  @Put(':id/status')
  @Roles('admin')
  async setStatus(@Param('id') id: string, @Body() dto: UpdateStatusDto) {
    return this.usersService.updateStatus(id, dto.status);
  }

  @Put(':id/reset-password')
  @Roles('admin')
  async resetPassword(@Param('id') id: string, @Body() dto: ResetPasswordDto) {
    await this.usersService.resetPassword(id, dto.newPassword);
    return { message: '密码已重置' };
  }
}
