import {
  Body,
  Controller,
  Get,
  Patch,
  Post,
  Request,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { BaselineDto } from './dto/baseline.dto';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { OnboardingService } from './onboarding.service';

@Controller('onboarding')
@UseGuards(JwtAuthGuard)
export class OnboardingController {
  constructor(private readonly onboardingService: OnboardingService) {}

  @Get('status')
  async getStatus(@Request() req: any) {
    return this.onboardingService.getStatus(req.user.sub);
  }

  @Patch('profile')
  async updateProfile(
    @Request() req: any,
    @Body() dto: UpdateProfileDto,
  ) {
    return this.onboardingService.updateProfile(req.user.sub, dto);
  }

  @Post('baseline')
  async baseline(@Request() req: any, @Body() dto: BaselineDto) {
    return this.onboardingService.saveBaseline(req.user.sub, dto);
  }

  @Post('finalize')
  async finalize(@Request() req: any) {
    return this.onboardingService.finalize(req.user.sub);
  }
}
