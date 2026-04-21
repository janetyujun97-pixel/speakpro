import { Body, Controller, HttpCode, Post } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AppleSigninDto } from './dto/apple-signin.dto';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { RegisterPhoneDto } from './dto/register-phone.dto';
import { RequestEmailResetDto } from './dto/request-email-reset.dto';
import { RequestResetDto } from './dto/request-reset.dto';
import { ResetEmailPasswordDto } from './dto/reset-email-password.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';
import { SendOtpDto } from './dto/send-otp.dto';
import { VerifyOtpDto } from './dto/verify-otp.dto';
import { WechatSigninDto } from './dto/wechat-signin.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  // ==================== 邮箱 ====================

  @Post('login')
  async login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('register')
  async register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('refresh')
  async refresh(@Body('refreshToken') refreshToken: string) {
    return this.authService.refreshToken(refreshToken);
  }

  // ==================== 手机 OTP ====================

  @Post('send-otp')
  @HttpCode(200)
  async sendOtp(@Body() dto: SendOtpDto) {
    return this.authService.sendOtp(dto.phone);
  }

  @Post('verify-otp')
  @HttpCode(200)
  async verifyOtp(@Body() dto: VerifyOtpDto) {
    return this.authService.verifyOtp(dto.phone, dto.code);
  }

  @Post('register-phone')
  async registerPhone(@Body() dto: RegisterPhoneDto) {
    return this.authService.registerWithPhone(dto);
  }

  @Post('request-reset')
  @HttpCode(200)
  async requestReset(@Body() dto: RequestResetDto) {
    // 复用 sendOtp：重置流程也是先下发验证码
    return this.authService.sendOtp(dto.phone);
  }

  @Post('reset-password')
  @HttpCode(200)
  async resetPassword(@Body() dto: ResetPasswordDto) {
    return this.authService.resetPasswordByPhone(
      dto.phone,
      dto.code,
      dto.newPassword,
    );
  }

  // ==================== 三方 ====================

  @Post('apple')
  async apple(@Body() dto: AppleSigninDto) {
    return this.authService.signInWithApple(dto.identityToken, dto.name);
  }

  @Post('wechat')
  async wechat(@Body() dto: WechatSigninDto) {
    return this.authService.signInWithWechat(dto.code);
  }

  // ==================== 教师端邮箱 reset ====================

  @Post('request-email-reset')
  @HttpCode(200)
  async requestEmailReset(@Body() dto: RequestEmailResetDto) {
    return this.authService.requestEmailReset(dto.email);
  }

  @Post('reset-email-password')
  @HttpCode(200)
  async resetEmailPassword(@Body() dto: ResetEmailPasswordDto) {
    return this.authService.resetPasswordByEmailToken(
      dto.token,
      dto.newPassword,
    );
  }
}
