import { IsNotEmpty, IsString, Length, Matches } from 'class-validator';

const CN_PHONE_RE = /^1[3-9]\d{9}$/;

export class VerifyOtpDto {
  @IsString()
  @IsNotEmpty({ message: '手机号不能为空' })
  @Matches(CN_PHONE_RE, { message: '手机号格式不正确' })
  phone: string;

  @IsString()
  @IsNotEmpty({ message: '验证码不能为空' })
  @Length(6, 6, { message: '验证码为 6 位数字' })
  @Matches(/^\d{6}$/, { message: '验证码为 6 位数字' })
  code: string;
}
