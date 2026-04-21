import { IsNotEmpty, IsString, Matches } from 'class-validator';

// 中国大陆手机号
const CN_PHONE_RE = /^1[3-9]\d{9}$/;

export class SendOtpDto {
  @IsString()
  @IsNotEmpty({ message: '手机号不能为空' })
  @Matches(CN_PHONE_RE, { message: '手机号格式不正确' })
  phone: string;
}
