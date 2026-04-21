import {
  BadRequestException,
  ConflictException,
  Inject,
  Injectable,
  Logger,
  NotFoundException,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { randomBytes, randomInt } from 'crypto';
import { IsNull, LessThan, Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';

import { RedisService } from '../../common/redis/redis.service';
import { UsersService } from '../users/users.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { AppleProvider } from './providers/apple.provider';
import { MailProvider } from './providers/mail.provider';
import {
  IOtpProvider,
  OTP_PROVIDER,
} from './providers/otp.provider';
import { WechatProvider } from './providers/wechat.provider';
import { EmailResetToken } from './entities/email-reset-token.entity';

// —— OTP 约束 ——
const OTP_TTL_SEC = 300;          // 验证码 5 分钟有效
const OTP_COOLDOWN_SEC = 60;      // 同号 60 秒冷却
const OTP_MAX_FAIL = 5;           // 单号连续 5 次失败
const OTP_LOCK_SEC = 30 * 60;     // 锁 30 分钟

const EMAIL_RESET_TTL_MS = 30 * 60 * 1000; // 邮件 reset token 30 分钟有效

interface UserLike {
  id: string;
  email?: string | null;
  phone?: string | null;
  name: string;
  role: string;
  password?: string;
  avatarUrl?: string | null;
}

@Injectable()
export class AuthService {
  private readonly logger = new Logger('AuthService');

  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
    private readonly redis: RedisService,
    private readonly config: ConfigService,
    @Inject(OTP_PROVIDER) private readonly otpProvider: IOtpProvider,
    private readonly appleProvider: AppleProvider,
    private readonly wechatProvider: WechatProvider,
    private readonly mailProvider: MailProvider,
    @InjectRepository(EmailResetToken)
    private readonly resetTokens: Repository<EmailResetToken>,
  ) {}

  // ==================== 已有：邮箱密码 ====================

  async register(registerDto: RegisterDto) {
    const existing = await this.usersService.findByEmail(registerDto.email);
    if (existing) {
      throw new ConflictException('该邮箱已注册');
    }

    const hashedPassword = await bcrypt.hash(registerDto.password, 10);
    const user = await this.usersService.create({
      name: registerDto.name,
      email: registerDto.email,
      password: hashedPassword,
      role: registerDto.role || 'student',
    });

    return this.signResponse(user);
  }

  async login(loginDto: LoginDto) {
    const user = await this.usersService.findByEmail(loginDto.email);
    if (!user || !user.password) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    const ok = await bcrypt.compare(loginDto.password, user.password);
    if (!ok) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    return this.signResponse(user);
  }

  async refreshToken(refreshToken: string) {
    try {
      const payload = this.jwtService.verify(refreshToken);
      return this.generateTokens(payload.sub, payload.email, payload.role);
    } catch {
      throw new UnauthorizedException('Refresh Token 无效或已过期');
    }
  }

  // ==================== 新增：手机 OTP ====================

  /** 下发验证码：60s 冷却 + 30min 锁 + 持久审计 */
  async sendOtp(phone: string): Promise<{ cooldownSec: number }> {
    // 锁检查
    if (await this.redis.exists(this.lockKey(phone))) {
      const ttl = await this.redis.ttl(this.lockKey(phone));
      throw new BadRequestException(
        `该号码已因多次失败被锁定，请 ${Math.ceil(ttl / 60)} 分钟后再试`,
      );
    }

    // 冷却检查
    if (await this.redis.exists(this.cooldownKey(phone))) {
      throw new BadRequestException('发送过于频繁，请稍候再试');
    }

    // 生成 6 位数字
    const code = String(randomInt(0, 1_000_000)).padStart(6, '0');
    await this.redis.setex(this.otpKey(phone), OTP_TTL_SEC, code);
    await this.redis.setex(this.cooldownKey(phone), OTP_COOLDOWN_SEC, '1');

    try {
      await this.otpProvider.send(phone, code);
    } catch (err: any) {
      this.logger.error(`OTP 下发失败 phone=${phone}: ${err?.message}`);
      await this.redis.del([this.otpKey(phone), this.cooldownKey(phone)]);
      throw new ServiceUnavailableException('验证码下发失败，请稍后重试');
    }

    return { cooldownSec: OTP_COOLDOWN_SEC };
  }

  /** 仅校验 OTP，不销毁（register/reset 流程里二次校验时会销毁） */
  async verifyOtp(phone: string, code: string): Promise<{ ok: true }> {
    await this.checkOtpOrThrow(phone, code);
    return { ok: true };
  }

  /** 手机号注册；校验 OTP 后消费 */
  async registerWithPhone(params: {
    phone: string;
    code: string;
    name: string;
    password?: string;
  }) {
    await this.checkOtpOrThrow(params.phone, params.code);

    const existing = await this.usersService.findByPhone(params.phone);
    if (existing) {
      throw new ConflictException('该手机号已注册');
    }

    const hashedPassword = params.password
      ? await bcrypt.hash(params.password, 10)
      : undefined;

    const user = await this.usersService.create({
      name: params.name,
      phone: params.phone,
      password: hashedPassword,
      role: 'student',
    });

    await this.consumeOtp(params.phone);
    return this.signResponse(user);
  }

  /** 手机号 + OTP 重置密码 */
  async resetPasswordByPhone(phone: string, code: string, newPassword: string) {
    await this.checkOtpOrThrow(phone, code);
    const user = await this.usersService.findByPhone(phone);
    if (!user) {
      throw new NotFoundException('手机号未注册');
    }
    await this.usersService.setPassword(user.id, newPassword);
    await this.consumeOtp(phone);
    return { ok: true };
  }

  // ==================== 新增：Apple / WeChat ====================

  async signInWithApple(identityToken: string, nameHint?: string) {
    if (!this.appleProvider.isConfigured()) {
      // 当前阶段无凭证 —— 端点直接 501，由 Controller 处理
      throw new ServiceUnavailableException('Apple 登录尚未配置');
    }

    const payload = await this.appleProvider.verify(identityToken);

    let user = await this.usersService.findByAppleSub(payload.sub);
    if (!user && payload.email) {
      // 若用户之前用同一邮箱注册过，绑定 apple_sub 而不是建新号
      const byEmail = await this.usersService.findByEmail(payload.email);
      if (byEmail) {
        user = await this.usersService.update(byEmail.id, {
          appleSub: payload.sub,
        });
      }
    }

    if (!user) {
      user = await this.usersService.create({
        role: 'student',
        name: nameHint || 'Apple User',
        email: payload.email || undefined,
        appleSub: payload.sub,
      });
    }

    return this.signResponse(user);
  }

  async signInWithWechat(code: string) {
    if (!this.wechatProvider.isConfigured()) {
      throw new ServiceUnavailableException('WeChat 登录尚未配置');
    }

    const profile = await this.wechatProvider.exchangeCode(code);

    let user = await this.usersService.findByWechatUnionid(profile.unionid);
    if (!user) {
      user = await this.usersService.create({
        role: 'student',
        name: profile.nickname || 'WeChat User',
        wechatUnionid: profile.unionid,
        avatarUrl: profile.avatarUrl,
      });
    }

    return this.signResponse(user);
  }

  // ==================== 新增：邮箱 reset（教师端） ====================

  /** 请求邮箱重置链接：无论邮箱是否存在都返回 200，避免枚举账号 */
  async requestEmailReset(email: string) {
    const user = await this.usersService.findByEmail(email);
    if (user) {
      // 旧 token 失效
      await this.resetTokens.update(
        { userId: user.id, consumedAt: IsNull() },
        { consumedAt: new Date() },
      );

      const token = randomBytes(32).toString('hex'); // 64 字符
      const expiresAt = new Date(Date.now() + EMAIL_RESET_TTL_MS);
      await this.resetTokens.insert({
        token,
        userId: user.id,
        email: user.email,
        expiresAt,
        consumedAt: null,
      });

      const base = this.config.get<string>(
        'WEB_URL',
        'http://localhost:3001',
      );
      const resetUrl = `${base.replace(/\/$/, '')}/reset-password?token=${token}`;
      await this.mailProvider.sendResetEmail(user.email, resetUrl);
    }
    return { ok: true };
  }

  /** 用邮件 token 重置密码 */
  async resetPasswordByEmailToken(token: string, newPassword: string) {
    const row = await this.resetTokens.findOne({ where: { token } });
    if (!row || row.consumedAt || row.expiresAt.getTime() < Date.now()) {
      throw new BadRequestException('链接无效或已过期，请重新申请');
    }
    await this.usersService.setPassword(row.userId, newPassword);
    await this.resetTokens.update({ token }, { consumedAt: new Date() });
    return { ok: true };
  }

  /** 清理过期 reset token —— 非 cron，懒调用即可（service 层定时清理可后续补） */
  async cleanupExpiredResetTokens() {
    await this.resetTokens.delete({ expiresAt: LessThan(new Date()) });
  }

  // ==================== 内部工具 ====================

  private async checkOtpOrThrow(phone: string, code: string): Promise<void> {
    if (await this.redis.exists(this.lockKey(phone))) {
      throw new BadRequestException('该号码已被锁定，请稍后再试');
    }

    const saved = await this.redis.get(this.otpKey(phone));
    if (!saved) {
      throw new BadRequestException('验证码已过期，请重新获取');
    }

    if (saved !== code) {
      const fails = await this.redis.incr(this.failKey(phone));
      await this.redis.expire(this.failKey(phone), OTP_LOCK_SEC);
      if (fails >= OTP_MAX_FAIL) {
        await this.redis.setex(this.lockKey(phone), OTP_LOCK_SEC, '1');
        await this.redis.del([this.otpKey(phone), this.failKey(phone)]);
      }
      throw new BadRequestException('验证码错误');
    }
  }

  private async consumeOtp(phone: string): Promise<void> {
    await this.redis.del([
      this.otpKey(phone),
      this.failKey(phone),
      this.cooldownKey(phone),
    ]);
  }

  private otpKey(phone: string) {
    return `otp:${phone}`;
  }
  private cooldownKey(phone: string) {
    return `otp:cooldown:${phone}`;
  }
  private failKey(phone: string) {
    return `otp:fail:${phone}`;
  }
  private lockKey(phone: string) {
    return `otp:lock:${phone}`;
  }

  private signResponse(user: UserLike) {
    const tokens = this.generateTokens(user.id, user.email ?? '', user.role);
    return {
      user: {
        id: user.id,
        name: user.name,
        email: user.email ?? null,
        phone: user.phone ?? null,
        role: user.role,
        avatarUrl: user.avatarUrl ?? null,
      },
      ...tokens,
    };
  }

  private generateTokens(userId: string, email: string, role: string) {
    const payload = { sub: userId, email, role };
    return {
      accessToken: this.jwtService.sign(payload, { expiresIn: '2h' }),
      refreshToken: this.jwtService.sign(payload, { expiresIn: '7d' }),
    };
  }
}
