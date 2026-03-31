import {
  Controller,
  Get,
  Post,
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

  @Post(':id/students')
  async addStudent(
    @Param('id') id: string,
    @Body('studentId') studentId: string,
  ) {
    return this.classesService.addStudent(id, studentId);
  }

  @Get(':id/analytics')
  async getAnalytics(@Param('id') id: string) {
    return this.classesService.getAnalytics(id);
  }
}
