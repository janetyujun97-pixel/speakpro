import {
  IsEmail,
  IsString,
  IsIn,
  IsOptional,
  IsNotEmpty,
  MinLength,
} from 'class-validator';

// 管理员编辑账号基本信息
export class UpdateUserDto {
  @IsString()
  @IsOptional()
  @IsNotEmpty({ message: '姓名不能为空' })
  name?: string;

  @IsEmail({}, { message: '请输入有效的邮箱地址' })
  @IsOptional()
  email?: string;

  @IsString()
  @IsOptional()
  @IsIn(['student', 'teacher', 'admin'], { message: '角色必须为 student、teacher 或 admin' })
  role?: 'student' | 'teacher' | 'admin';
}

// 启用/禁用账号
export class UpdateStatusDto {
  @IsString()
  @IsIn(['active', 'disabled'], { message: '状态必须为 active 或 disabled' })
  status: 'active' | 'disabled';
}

// 管理员重置密码
export class ResetPasswordDto {
  @IsString()
  @IsNotEmpty({ message: '新密码不能为空' })
  @MinLength(6, { message: '密码长度不能少于6位' })
  newPassword: string;
}
