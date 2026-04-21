import { IsNotEmpty, IsString, Matches } from 'class-validator';

const CN_PHONE_RE = /^1[3-9]\d{9}$/;

export class RequestResetDto {
  @IsString()
  @IsNotEmpty({ message: '手机号不能为空' })
  @Matches(CN_PHONE_RE, { message: '手机号格式不正确' })
  phone: string;
}
