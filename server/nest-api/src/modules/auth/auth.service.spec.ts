import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { getRepositoryToken } from '@nestjs/typeorm';
import {
  BadRequestException,
  ConflictException,
  NotFoundException,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { AuthService } from './auth.service';
import { UsersService } from '../users/users.service';
import { RedisService } from '../../common/redis/redis.service';
import { AppleProvider } from './providers/apple.provider';
import { WechatProvider } from './providers/wechat.provider';
import { MailProvider } from './providers/mail.provider';
import { OTP_PROVIDER } from './providers/otp.provider';
import { EmailResetToken } from './entities/email-reset-token.entity';

describe('AuthService', () => {
  let authService: AuthService;
  let usersService: Record<string, jest.Mock>;
  let jwtService: Record<string, jest.Mock>;
  let redis: {
    get: jest.Mock;
    set: jest.Mock;
    setex: jest.Mock;
    del: jest.Mock;
    incr: jest.Mock;
    expire: jest.Mock;
    ttl: jest.Mock;
    exists: jest.Mock;
  };
  let otpProvider: { send: jest.Mock };
  let appleProvider: { isConfigured: jest.Mock; verify: jest.Mock };
  let wechatProvider: { isConfigured: jest.Mock; exchangeCode: jest.Mock };
  let mailProvider: { sendResetEmail: jest.Mock };
  let resetRepo: {
    insert: jest.Mock;
    update: jest.Mock;
    findOne: jest.Mock;
    delete: jest.Mock;
  };

  beforeEach(async () => {
    usersService = {
      findByEmail: jest.fn(),
      findByPhone: jest.fn(),
      findByAppleSub: jest.fn(),
      findByWechatUnionid: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      setPassword: jest.fn(),
    };

    jwtService = {
      sign: jest.fn().mockReturnValue('mock-token'),
      verify: jest.fn(),
    };

    redis = {
      get: jest.fn(),
      set: jest.fn(),
      setex: jest.fn(),
      del: jest.fn(),
      incr: jest.fn(),
      expire: jest.fn(),
      ttl: jest.fn(),
      exists: jest.fn().mockResolvedValue(false),
    };

    otpProvider = { send: jest.fn().mockResolvedValue(undefined) };
    appleProvider = {
      isConfigured: jest.fn().mockReturnValue(false),
      verify: jest.fn(),
    };
    wechatProvider = {
      isConfigured: jest.fn().mockReturnValue(false),
      exchangeCode: jest.fn(),
    };
    mailProvider = { sendResetEmail: jest.fn().mockResolvedValue(undefined) };
    resetRepo = {
      insert: jest.fn(),
      update: jest.fn(),
      findOne: jest.fn(),
      delete: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: UsersService, useValue: usersService },
        { provide: JwtService, useValue: jwtService },
        { provide: RedisService, useValue: redis },
        {
          provide: ConfigService,
          useValue: { get: jest.fn((_key: string, def?: unknown) => def) },
        },
        { provide: OTP_PROVIDER, useValue: otpProvider },
        { provide: AppleProvider, useValue: appleProvider },
        { provide: WechatProvider, useValue: wechatProvider },
        { provide: MailProvider, useValue: mailProvider },
        { provide: getRepositoryToken(EmailResetToken), useValue: resetRepo },
      ],
    }).compile();

    authService = module.get<AuthService>(AuthService);
  });

  // ---- register（邮箱） ----

  describe('register', () => {
    const dto = { name: 'Test', email: 'test@example.com', password: 'pass123' };

    it('应注册新用户并返回 tokens', async () => {
      usersService.findByEmail.mockResolvedValue(null);
      usersService.create.mockResolvedValue({
        id: 'u1', name: 'Test', email: 'test@example.com', role: 'student',
      });

      const result = await authService.register(dto);

      expect(usersService.findByEmail).toHaveBeenCalledWith('test@example.com');
      expect(usersService.create).toHaveBeenCalled();
      expect(result.accessToken).toBe('mock-token');
      expect(result.user.email).toBe('test@example.com');
    });

    it('邮箱已注册时应抛出 ConflictException', async () => {
      usersService.findByEmail.mockResolvedValue({ id: 'existing' });
      await expect(authService.register(dto)).rejects.toThrow(ConflictException);
    });

    it('密码应使用 bcrypt 哈希', async () => {
      usersService.findByEmail.mockResolvedValue(null);
      usersService.create.mockImplementation(async (data) => {
        const isHashed = await bcrypt.compare('pass123', data.password);
        expect(isHashed).toBe(true);
        return { id: 'u1', ...data };
      });
      await authService.register(dto);
    });
  });

  // ---- login（邮箱） ----

  describe('login', () => {
    const dto = { email: 'test@example.com', password: 'pass123' };

    it('正确凭据应返回 tokens', async () => {
      const hashedPwd = await bcrypt.hash('pass123', 10);
      usersService.findByEmail.mockResolvedValue({
        id: 'u1', name: 'Test', email: 'test@example.com', password: hashedPwd, role: 'student',
      });
      const result = await authService.login(dto);
      expect(result.accessToken).toBe('mock-token');
      expect(result.user.id).toBe('u1');
    });

    it('用户不存在时应抛出 UnauthorizedException', async () => {
      usersService.findByEmail.mockResolvedValue(null);
      await expect(authService.login(dto)).rejects.toThrow(UnauthorizedException);
    });

    it('密码错误时应抛出 UnauthorizedException', async () => {
      const hashedPwd = await bcrypt.hash('different', 10);
      usersService.findByEmail.mockResolvedValue({
        id: 'u1', email: 'test@example.com', password: hashedPwd, role: 'student',
      });
      await expect(authService.login(dto)).rejects.toThrow(UnauthorizedException);
    });
  });

  // ---- refreshToken ----

  describe('refreshToken', () => {
    it('有效 token 应返回新 tokens', async () => {
      jwtService.verify.mockReturnValue({ sub: 'u1', email: 'test@example.com', role: 'student' });
      const result = await authService.refreshToken('valid-token');
      expect(result.accessToken).toBe('mock-token');
    });

    it('无效 token 应抛出 UnauthorizedException', async () => {
      jwtService.verify.mockImplementation(() => { throw new Error('invalid'); });
      await expect(authService.refreshToken('bad-token')).rejects.toThrow(UnauthorizedException);
    });
  });

  // ---- sendOtp ----

  describe('sendOtp', () => {
    it('正常下发后写入 otp 和 cooldown 键', async () => {
      redis.exists.mockResolvedValue(false);
      const res = await authService.sendOtp('13812345678');
      expect(redis.setex).toHaveBeenCalledWith(
        'otp:13812345678',
        300,
        expect.stringMatching(/^\d{6}$/),
      );
      expect(redis.setex).toHaveBeenCalledWith('otp:cooldown:13812345678', 60, '1');
      expect(otpProvider.send).toHaveBeenCalled();
      expect(res.cooldownSec).toBe(60);
    });

    it('冷却期内抛出 BadRequest', async () => {
      redis.exists.mockImplementation(async (k: string) => k.startsWith('otp:cooldown:'));
      await expect(authService.sendOtp('13812345678')).rejects.toThrow(BadRequestException);
    });

    it('锁定期内抛出 BadRequest 并带剩余时间', async () => {
      redis.exists.mockImplementation(async (k: string) => k.startsWith('otp:lock:'));
      redis.ttl.mockResolvedValue(600);
      await expect(authService.sendOtp('13812345678')).rejects.toThrow(BadRequestException);
    });

    it('下发失败回滚 otp/cooldown 键', async () => {
      redis.exists.mockResolvedValue(false);
      otpProvider.send.mockRejectedValue(new Error('provider down'));
      await expect(authService.sendOtp('13812345678')).rejects.toThrow(
        ServiceUnavailableException,
      );
      expect(redis.del).toHaveBeenCalled();
    });
  });

  // ---- registerWithPhone ----

  describe('registerWithPhone', () => {
    const params = {
      phone: '13812345678',
      code: '123456',
      name: 'Phone User',
      password: 'pwd123',
    };

    beforeEach(() => {
      redis.exists.mockResolvedValue(false);
      redis.get.mockResolvedValue('123456');
    });

    it('OTP 正确时应建号并返回 tokens', async () => {
      usersService.findByPhone.mockResolvedValue(null);
      usersService.create.mockResolvedValue({
        id: 'u2', name: 'Phone User', phone: '13812345678', role: 'student',
      });
      const res = await authService.registerWithPhone(params);
      expect(res.accessToken).toBe('mock-token');
      expect(res.user.phone).toBe('13812345678');
      expect(redis.del).toHaveBeenCalled();
    });

    it('OTP 错误时应抛 BadRequest 并递增 fail', async () => {
      redis.get.mockResolvedValue('999999');
      redis.incr.mockResolvedValue(1);
      await expect(authService.registerWithPhone(params)).rejects.toThrow(BadRequestException);
      expect(redis.incr).toHaveBeenCalledWith('otp:fail:13812345678');
    });

    it('OTP 连续错误到上限应设置 lock', async () => {
      redis.get.mockResolvedValue('999999');
      redis.incr.mockResolvedValue(5);
      await expect(authService.registerWithPhone(params)).rejects.toThrow(BadRequestException);
      expect(redis.setex).toHaveBeenCalledWith('otp:lock:13812345678', 1800, '1');
    });

    it('手机号已注册时应抛 Conflict', async () => {
      usersService.findByPhone.mockResolvedValue({ id: 'old' });
      await expect(authService.registerWithPhone(params)).rejects.toThrow(ConflictException);
    });
  });

  // ---- resetPasswordByPhone ----

  describe('resetPasswordByPhone', () => {
    beforeEach(() => {
      redis.exists.mockResolvedValue(false);
      redis.get.mockResolvedValue('123456');
    });

    it('正常流程应写入新密码', async () => {
      usersService.findByPhone.mockResolvedValue({ id: 'u1' });
      await authService.resetPasswordByPhone('13812345678', '123456', 'newpwd123');
      expect(usersService.setPassword).toHaveBeenCalledWith('u1', 'newpwd123');
    });

    it('手机号未注册应抛 NotFound', async () => {
      usersService.findByPhone.mockResolvedValue(null);
      await expect(
        authService.resetPasswordByPhone('13812345678', '123456', 'newpwd123'),
      ).rejects.toThrow(NotFoundException);
    });
  });

  // ---- signInWithApple / signInWithWechat ----

  describe('signInWithApple', () => {
    it('未配置凭证时抛 ServiceUnavailable', async () => {
      appleProvider.isConfigured.mockReturnValue(false);
      await expect(authService.signInWithApple('token')).rejects.toThrow(
        ServiceUnavailableException,
      );
    });

    it('凭证有效时创建或登录用户', async () => {
      appleProvider.isConfigured.mockReturnValue(true);
      appleProvider.verify.mockResolvedValue({ sub: 'apple_123', email: 'a@b.c' });
      usersService.findByAppleSub.mockResolvedValue(null);
      usersService.findByEmail.mockResolvedValue(null);
      usersService.create.mockResolvedValue({
        id: 'u3', name: 'Apple User', role: 'student', appleSub: 'apple_123',
      });
      const res = await authService.signInWithApple('token', 'Apple User');
      expect(res.accessToken).toBe('mock-token');
    });
  });

  describe('signInWithWechat', () => {
    it('未配置凭证时抛 ServiceUnavailable', async () => {
      wechatProvider.isConfigured.mockReturnValue(false);
      await expect(authService.signInWithWechat('code')).rejects.toThrow(
        ServiceUnavailableException,
      );
    });
  });

  // ---- requestEmailReset ----

  describe('requestEmailReset', () => {
    it('未注册邮箱也返回 ok (不暴露枚举)', async () => {
      usersService.findByEmail.mockResolvedValue(null);
      const res = await authService.requestEmailReset('noone@x.com');
      expect(res.ok).toBe(true);
      expect(resetRepo.insert).not.toHaveBeenCalled();
      expect(mailProvider.sendResetEmail).not.toHaveBeenCalled();
    });

    it('已注册邮箱写入 token 并发邮件', async () => {
      usersService.findByEmail.mockResolvedValue({ id: 'u1', email: 'a@b.c' });
      await authService.requestEmailReset('a@b.c');
      expect(resetRepo.update).toHaveBeenCalled();
      expect(resetRepo.insert).toHaveBeenCalled();
      expect(mailProvider.sendResetEmail).toHaveBeenCalled();
    });
  });

  // ---- resetPasswordByEmailToken ----

  describe('resetPasswordByEmailToken', () => {
    it('未知 token 抛 BadRequest', async () => {
      resetRepo.findOne.mockResolvedValue(null);
      await expect(
        authService.resetPasswordByEmailToken('x', 'newpwd'),
      ).rejects.toThrow(BadRequestException);
    });

    it('过期 token 抛 BadRequest', async () => {
      resetRepo.findOne.mockResolvedValue({
        token: 'x',
        userId: 'u1',
        expiresAt: new Date(Date.now() - 1000),
        consumedAt: null,
      });
      await expect(
        authService.resetPasswordByEmailToken('x', 'newpwd'),
      ).rejects.toThrow(BadRequestException);
    });

    it('已消费 token 抛 BadRequest', async () => {
      resetRepo.findOne.mockResolvedValue({
        token: 'x',
        userId: 'u1',
        expiresAt: new Date(Date.now() + 60_000),
        consumedAt: new Date(),
      });
      await expect(
        authService.resetPasswordByEmailToken('x', 'newpwd'),
      ).rejects.toThrow(BadRequestException);
    });

    it('正常 token 写入新密码并标记 consumed', async () => {
      resetRepo.findOne.mockResolvedValue({
        token: 'x',
        userId: 'u1',
        expiresAt: new Date(Date.now() + 60_000),
        consumedAt: null,
      });
      const res = await authService.resetPasswordByEmailToken('x', 'newpwd');
      expect(usersService.setPassword).toHaveBeenCalledWith('u1', 'newpwd');
      expect(resetRepo.update).toHaveBeenCalled();
      expect(res.ok).toBe(true);
    });
  });
});
