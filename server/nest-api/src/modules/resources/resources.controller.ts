import {
  Controller,
  Get,
  Post,
  Delete,
  Body,
  Param,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ResourcesService } from './resources.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';

@Controller('resources')
@UseGuards(JwtAuthGuard, RolesGuard)
export class ResourcesController {
  constructor(private readonly resourcesService: ResourcesService) {}

  @Post('upload-sign')
  @Roles('teacher', 'admin')
  async getUploadSignature(
    @Body() data: { filename: string; contentType: string },
  ) {
    return this.resourcesService.getUploadSignature(data);
  }

  @Post()
  @Roles('teacher', 'admin')
  async create(@Body() data: any, @Request() req: any) {
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

  @Delete(':id')
  @Roles('teacher', 'admin')
  async delete(@Param('id') id: string) {
    return this.resourcesService.delete(id);
  }
}
