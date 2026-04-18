import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { JwtStrategy } from './auth.strategy';
import { EmailResetToken } from './entities/email-reset-token.entity';
import { AppleProvider } from './providers/apple.provider';
import { MailProvider } from './providers/mail.provider';
import {
  AliyunOtpProvider,
  LogOtpProvider,
  OTP_PROVIDER,
  otpProviderFactory,
} from './providers/otp.provider';
import { WechatProvider } from './providers/wechat.provider';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [
    UsersModule,
    TypeOrmModule.forFeature([EmailResetToken]),
    PassportModule.register({ defaultStrategy: 'jwt' }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET', 'speakpro_jwt_secret'),
        signOptions: { expiresIn: '2h' },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [
    AuthService,
    JwtStrategy,
    AppleProvider,
    WechatProvider,
    MailProvider,
    LogOtpProvider,
    AliyunOtpProvider,
    {
      provide: OTP_PROVIDER,
      useFactory: otpProviderFactory,
      inject: [ConfigService, LogOtpProvider, AliyunOtpProvider],
    },
  ],
  exports: [AuthService],
})
export class AuthModule {}
