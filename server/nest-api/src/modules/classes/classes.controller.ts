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

@Controller('classes')
@UseGuards(JwtAuthGuard)
export class ClassesController {
  constructor(private readonly classesService: ClassesService) {}

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

  @Put(':id')
  async update(@Param('id') id: string, @Body() data: { name?: string; examType?: string }) {
    return this.classesService.update(id, data);
  }

  @Delete(':id')
  async delete(@Param('id') id: string) {
    await this.classesService.delete(id);
    return { message: '班级已删除' };
  }

  @Post(':id/students')
  async addStudent(
    @Param('id') id: string,
    @Body('studentId') studentId: string,
  ) {
    return this.classesService.addStudent(id, studentId);
  }

  @Delete(':id/students/:studentId')
  async removeStudent(
    @Param('id') id: string,
    @Param('studentId') studentId: string,
  ) {
    return this.classesService.removeStudent(id, studentId);
  }

  @Get(':id/analytics')
  async getAnalytics(@Param('id') id: string) {
    return this.classesService.getAnalytics(id);
  }

  @Get(':id/score-trends')
  async getScoreTrends(@Param('id') id: string) {
    return this.classesService.getScoreTrends(id);
  }

  @Get(':id/leaderboard')
  async getStudentLeaderboard(@Param('id') id: string) {
    return this.classesService.getStudentLeaderboard(id);
  }
}
