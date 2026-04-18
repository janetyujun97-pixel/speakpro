import { Inject, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

/**
 * OTP 下发抽象。生产环境用 Aliyun，dev/测试用 LogOtpProvider 把验证码打到日志。
 */
export interface IOtpProvider {
  send(phone: string, code: string): Promise<void>;
}

export const OTP_PROVIDER = Symbol('OTP_PROVIDER');

@Injectable()
export class LogOtpProvider implements IOtpProvider {
  private readonly logger = new Logger('LogOtpProvider');

  async send(phone: string, code: string): Promise<void> {
    this.logger.warn(`[DEV OTP] phone=${phone} code=${code}`);
  }
}

@Injectable()
export class AliyunOtpProvider implements IOtpProvider {
  private readonly logger = new Logger('AliyunOtpProvider');

  constructor(private readonly config: ConfigService) {}

  async send(phone: string, code: string): Promise<void> {
    // 真实实现接入阿里云短信 SDK（@alicloud/dysmsapi20170525）
    // 当前阶段用户侧尚无凭证，先留占位 —— 调用时抛错以避免误发。
    const accessKeyId = this.config.get<string>('ALIYUN_SMS_ACCESS_KEY_ID');
    const secret = this.config.get<string>('ALIYUN_SMS_ACCESS_KEY_SECRET');
    if (!accessKeyId || !secret) {
      throw new Error('阿里云短信凭证未配置');
    }
    this.logger.log(`Aliyun SMS to=${phone}, code length=${code.length}`);
    throw new Error('AliyunOtpProvider 尚未接入，请配置凭证并实现 SDK 调用');
  }
}

export function otpProviderFactory(
  config: ConfigService,
  logProvider: LogOtpProvider,
  aliyunProvider: AliyunOtpProvider,
): IOtpProvider {
  const mode = (config.get<string>('SMS_PROVIDER') || 'log').toLowerCase();
  return mode === 'aliyun' ? aliyunProvider : logProvider;
}
