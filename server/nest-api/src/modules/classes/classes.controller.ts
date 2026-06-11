import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ClassesService } from './classes.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';

// 班级模块整体仅教师/管理员可用；service 内对 teacher 做班级归属校验
@Controller('classes')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('teacher', 'admin')
export class ClassesController {
  constructor(private readonly classesService: ClassesService) {}

  // 从 JWT 提取调用方身份（admin 不受归属限制）
  private requester(req: any) {
    return { id: req.user.sub, role: req.user.role };
  }

  @Post()
  async create(@Body() data: any, @Request() req: any) {
    return this.classesService.create({
      ...data,
      teacherId: req.user.sub,
    });
  }

  @Get()
  async findAll(@Request() req: any) {
    // 教师查看自己的班级，管理员查看全部
    const teacherId = req.user.role === 'admin' ? undefined : req.user.sub;
    return this.classesService.findAll(teacherId);
  }

  @Get(':id')
  async findById(@Param('id') id: string, @Request() req: any) {
    return this.classesService.findById(id, this.requester(req));
  }

  @Put(':id')
  async update(
    @Param('id') id: string,
    @Body() data: { name?: string; examType?: string },
    @Request() req: any,
  ) {
    return this.classesService.update(id, data, this.requester(req));
  }

  @Delete(':id')
  async delete(@Param('id') id: string, @Request() req: any) {
    await this.classesService.delete(id, this.requester(req));
    return { message: '班级已删除' };
  }

  @Post(':id/students')
  async addStudent(
    @Param('id') id: string,
    @Body('studentId') studentId: string,
    @Request() req: any,
  ) {
    return this.classesService.addStudent(id, studentId, this.requester(req));
  }

  @Delete(':id/students/:studentId')
  async removeStudent(
    @Param('id') id: string,
    @Param('studentId') studentId: string,
    @Request() req: any,
  ) {
    return this.classesService.removeStudent(id, studentId, this.requester(req));
  }

  @Get(':id/analytics')
  async getAnalytics(@Param('id') id: string, @Request() req: any) {
    return this.classesService.getAnalytics(id, this.requester(req));
  }

  @Get(':id/score-trends')
  async getScoreTrends(@Param('id') id: string, @Request() req: any) {
    return this.classesService.getScoreTrends(id, this.requester(req));
  }

  @Get(':id/leaderboard')
  async getStudentLeaderboard(@Param('id') id: string, @Request() req: any) {
    return this.classesService.getStudentLeaderboard(id, this.requester(req));
  }
}
