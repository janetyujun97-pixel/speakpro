import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createRemoteJWKSet, jwtVerify } from 'jose';

export interface AppleIdentityPayload {
  sub: string;
  email?: string;
  emailVerified?: boolean;
}

/**
 * 校验 Apple Sign-In 的 identityToken。
 * - 用 Apple 官方 JWKS (https://appleid.apple.com/auth/keys)
 * - 校验 issuer / audience（bundle id 或 service id）
 * - 返回 sub（即 apple_sub）
 *
 * 当前阶段用户侧尚无 APPLE_CLIENT_ID，Controller 会在未配置时返回 501。
 */
@Injectable()
export class AppleProvider {
  private readonly logger = new Logger('AppleProvider');
  private readonly jwks = createRemoteJWKSet(
    new URL('https://appleid.apple.com/auth/keys'),
  );

  constructor(private readonly config: ConfigService) {}

  isConfigured(): boolean {
    return !!this.config.get<string>('APPLE_CLIENT_ID');
  }

  async verify(identityToken: string): Promise<AppleIdentityPayload> {
    const clientId = this.config.get<string>('APPLE_CLIENT_ID');
    if (!clientId) {
      throw new UnauthorizedException('Apple Sign-In 未配置');
    }

    try {
      const { payload } = await jwtVerify(identityToken, this.jwks, {
        issuer: 'https://appleid.apple.com',
        audience: clientId,
      });

      if (!payload.sub) {
        throw new UnauthorizedException('identityToken 缺少 sub');
      }

      return {
        sub: payload.sub as string,
        email: payload.email as string | undefined,
        emailVerified: payload.email_verified === true || payload.email_verified === 'true',
      };
    } catch (err: any) {
      this.logger.warn(`Apple identityToken 校验失败: ${err?.message}`);
      throw new UnauthorizedException('identityToken 无效');
    }
  }
}
