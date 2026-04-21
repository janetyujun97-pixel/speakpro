import {
  IsNotEmpty,
  IsOptional,
  IsString,
  Length,
  Matches,
  MinLength,
} from 'class-validator';

const CN_PHONE_RE = /^1[3-9]\d{9}$/;

export class RegisterPhoneDto {
  @IsString()
  @IsNotEmpty({ message: '手机号不能为空' })
  @Matches(CN_PHONE_RE, { message: '手机号格式不正确' })
  phone: string;

  @IsString()
  @IsNotEmpty({ message: '验证码不能为空' })
  @Length(6, 6)
  @Matches(/^\d{6}$/)
  code: string;

  @IsString()
  @IsNotEmpty({ message: '姓名不能为空' })
  name: string;

  @IsOptional()
  @IsString()
  @MinLength(6, { message: '密码长度不能少于 6 位' })
  password?: string;
}
