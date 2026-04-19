import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

/**
 * 校验 `X-Internal-Secret` header 是否等于 INTERNAL_SHARED_SECRET。
 * 用于服务间调用（Go orchestrator → NestJS webhook），不暴露给外部用户。
 * 若 secret 未配置，guard 会拒绝所有请求（避免开发环境误放行）。
 */
@Injectable()
export class InternalSecretGuard implements CanActivate {
  constructor(private readonly config: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const expected = this.config.get<string>('INTERNAL_SHARED_SECRET');
    if (!expected) {
      throw new UnauthorizedException(
        'Internal endpoint unavailable: INTERNAL_SHARED_SECRET not configured',
      );
    }

    const req = context.switchToHttp().getRequest();
    const got =
      req.headers['x-internal-secret'] ??
      req.headers['X-Internal-Secret'.toLowerCase()];

    // 使用常量时间比较，避免时序攻击
    if (typeof got !== 'string' || !timingSafeEqual(got, expected)) {
      throw new UnauthorizedException('Invalid internal secret');
    }
    return true;
  }
}

function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let result = 0;
  for (let i = 0; i < a.length; i++) {
    result |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return result === 0;
}
