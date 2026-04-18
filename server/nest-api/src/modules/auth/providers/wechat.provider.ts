import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

export interface WechatProfile {
  openid: string;
  unionid: string;
  nickname?: string;
  avatarUrl?: string;
}

/**
 * 微信开放平台登录：code → access_token → userinfo。
 * 文档：https://developers.weixin.qq.com/doc/oplatform/Mobile_App/WeChat_Login/Development_Guide.html
 * 当前用户侧尚无 WECHAT_APP_ID，Controller 会在未配置时返回 501。
 */
@Injectable()
export class WechatProvider {
  private readonly logger = new Logger('WechatProvider');

  constructor(private readonly config: ConfigService) {}

  isConfigured(): boolean {
    return !!this.config.get<string>('WECHAT_APP_ID') &&
           !!this.config.get<string>('WECHAT_APP_SECRET');
  }

  async exchangeCode(code: string): Promise<WechatProfile> {
    const appid = this.config.get<string>('WECHAT_APP_ID');
    const secret = this.config.get<string>('WECHAT_APP_SECRET');
    if (!appid || !secret) {
      throw new UnauthorizedException('WeChat 登录未配置');
    }

    try {
      // 1. code → access_token
      const tokenUrl =
        `https://api.weixin.qq.com/sns/oauth2/access_token` +
        `?appid=${encodeURIComponent(appid)}` +
        `&secret=${encodeURIComponent(secret)}` +
        `&code=${encodeURIComponent(code)}` +
        `&grant_type=authorization_code`;

      const tokenResp = await fetch(tokenUrl).then((r) => r.json() as Promise<any>);
      if (tokenResp.errcode) {
        this.logger.warn(`WeChat token error: ${JSON.stringify(tokenResp)}`);
        throw new UnauthorizedException('WeChat code 无效');
      }

      const { access_token, openid, unionid } = tokenResp;
      if (!unionid) {
        // 应用未绑定开放平台、或用户未授权 scope=snsapi_userinfo 时会缺失 unionid
        throw new UnauthorizedException('WeChat 响应缺少 unionid，请检查开放平台绑定');
      }

      // 2. 拉用户资料（昵称 / 头像）
      const userUrl =
        `https://api.weixin.qq.com/sns/userinfo` +
        `?access_token=${encodeURIComponent(access_token)}` +
        `&openid=${encodeURIComponent(openid)}`;
      const user = await fetch(userUrl).then((r) => r.json() as Promise<any>);

      return {
        openid,
        unionid,
        nickname: user.nickname,
        avatarUrl: user.headimgurl,
      };
    } catch (err: any) {
      if (err instanceof UnauthorizedException) throw err;
      this.logger.error(`WeChat exchange 失败: ${err?.message}`);
      throw new UnauthorizedException('WeChat 登录失败');
    }
  }
}
