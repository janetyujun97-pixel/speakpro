import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ResourcesService } from './resources.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@Controller('resources')
@UseGuards(JwtAuthGuard)
export class ResourcesController {
  constructor(private readonly resourcesService: ResourcesService) {}

  @Post('upload-sign')
  async getUploadSignature(
    @Body() data: { filename: string; contentType: string },
  ) {
    return this.resourcesService.getUploadSignature(data);
  }

  @Post()
  async create(@Body() data: any, @Request() req) {
    return this.resourcesService.create({
      ...data,
      uploadedBy: req.user.sub,
    });
  }

  @Get()
  async findAll(
    @Query('exam_type') examType?: string,
    @Query('type') type?: string,
  ) {
    return this.resourcesService.findAll({ examType, type });
  }
}
