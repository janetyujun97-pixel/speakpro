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
import { QuestionsService, QuestionFilters } from './questions.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { Question } from './entities/question.entity';

@Controller('questions')
@UseGuards(JwtAuthGuard)
export class QuestionsController {
  constructor(private readonly questionsService: QuestionsService) {}

  @Get()
  async findAll(
    @Query('exam_type') examType?: string,
    @Query('section') section?: string,
    @Query('difficulty') difficulty?: number,
  ): Promise<Question[]> {
    const filters: QuestionFilters = { examType, section, difficulty };
    return this.questionsService.findAll(filters);
  }

  @Post()
  async create(@Body() data: Partial<Question>, @Request() req): Promise<Question> {
    return this.questionsService.create({ ...data, createdBy: req.user.sub });
  }

  @Put(':id')
  async update(
    @Param('id') id: string,
    @Body() data: Partial<Question>,
  ): Promise<Question> {
    return this.questionsService.update(id, data);
  }

  @Delete(':id')
  async delete(@Param('id') id: string): Promise<void> {
    return this.questionsService.delete(id);
  }

  @Post('import')
  async bulkImport(@Body() questions: Partial<Question>[]): Promise<Question[]> {
    return this.questionsService.bulkImport(questions);
  }
}
