import { IsNotEmpty, IsString } from 'class-validator';

export class WechatSigninDto {
  // 微信 SDK 授权回调里拿到的 code，服务端换 access_token + openid + unionid
  @IsString()
  @IsNotEmpty({ message: 'code 不能为空' })
  code: string;
}
