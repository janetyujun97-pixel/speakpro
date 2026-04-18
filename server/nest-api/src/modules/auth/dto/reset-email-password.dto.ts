import { IsNotEmpty, IsString, MinLength } from 'class-validator';

export class ResetEmailPasswordDto {
  @IsString()
  @IsNotEmpty({ message: 'token 不能为空' })
  token: string;

  @IsString()
  @IsNotEmpty({ message: '新密码不能为空' })
  @MinLength(6, { message: '密码长度不能少于 6 位' })
  newPassword: string;
}
