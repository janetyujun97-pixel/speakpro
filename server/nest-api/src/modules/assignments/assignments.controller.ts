import {
  Controller,
  Get,
  Post,
  Put,
  Patch,
  Body,
  Param,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { AssignmentsService } from './assignments.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('assignments')
@UseGuards(JwtAuthGuard)
export class AssignmentsController {
  constructor(private readonly assignmentsService: AssignmentsService) {}

  @Post()
  async create(@Body() data: any, @Request() req: any) {
    return this.assignmentsService.create({
      ...data,
      teacherId: req.user.sub,
    });
  }

  @Get()
  async findAll(
    @Query('class_id') classId?: string,
    @Request() req?: any,
  ) {
    return this.assignmentsService.findAll({ classId });
  }

  @Get(':id')
  async findById(@Param('id') id: string) {
    return this.assignmentsService.findById(id);
  }

  @Get(':id/submissions/:sid')
  async getSubmissionDetail(
    @Param('id') id: string,
    @Param('sid') sid: string,
  ) {
    return this.assignmentsService.getSubmissionDetail(id, sid);
  }

  @Post(':id/submit')
  async submit(
    @Param('id') id: string,
    @Body() data: { sessionIds: string[] },
    @Request() req: any,
  ) {
    return this.assignmentsService.submit(id, req.user.sub, data);
  }

  @Put(':id/grade')
  async grade(
    @Param('id') id: string,
    @Body()
    data: {
      submissionId: string;
      teacherComment?: string;
      teacherScore: number;
      teacherVoiceUrl?: string;
    },
  ) {
    return this.assignmentsService.grade(id, data.submissionId, {
      teacherComment: data.teacherComment,
      teacherScore: data.teacherScore,
      teacherVoiceUrl: data.teacherVoiceUrl,
    });
  }

  /** Web 教师后台录完整体语音备注后回写作业 */
  @Patch(':id/teacher-voice')
  async updateTeacherVoice(
    @Param('id') id: string,
    @Body() data: { teacherVoiceUrl: string | null },
  ) {
    return this.assignmentsService.updateTeacherVoice(id, data.teacherVoiceUrl);
  }
}
