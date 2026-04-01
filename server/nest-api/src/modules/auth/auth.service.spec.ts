import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { ConflictException, UnauthorizedException } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { AuthService } from './auth.service';
import { UsersService } from '../users/users.service';

describe('AuthService', () => {
  let authService: AuthService;
  let usersService: Partial<Record<keyof UsersService, jest.Mock>>;
  let jwtService: Partial<Record<keyof JwtService, jest.Mock>>;

  beforeEach(async () => {
    usersService = {
      findByEmail: jest.fn(),
      create: jest.fn(),
    };

    jwtService = {
      sign: jest.fn().mockReturnValue('mock-token'),
      verify: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: UsersService, useValue: usersService },
        { provide: JwtService, useValue: jwtService },
      ],
    }).compile();

    authService = module.get<AuthService>(AuthService);
  });

  // ---- register ----

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
        // 验证传入的密码是哈希后的
        const isHashed = await bcrypt.compare('pass123', data.password);
        expect(isHashed).toBe(true);
        return { id: 'u1', ...data };
      });

      await authService.register(dto);
    });
  });

  // ---- login ----

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
      expect(result.refreshToken).toBe('mock-token');
    });

    it('无效 token 应抛出 UnauthorizedException', async () => {
      jwtService.verify.mockImplementation(() => { throw new Error('invalid'); });

      await expect(authService.refreshToken('bad-token')).rejects.toThrow(UnauthorizedException);
    });
  });
});
