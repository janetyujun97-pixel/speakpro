import {
  IsEmail,
  IsNotEmpty,
  IsString,
  MinLength,
  IsIn,
} from 'class-validator';

// 管理员后台创建账号（教师/学生/管理员）
export class CreateUserDto {
  @IsString()
  @IsNotEmpty({ message: '姓名不能为空' })
  name: string;

  @IsEmail({}, { message: '请输入有效的邮箱地址' })
  @IsNotEmpty({ message: '邮箱不能为空' })
  email: string;

  @IsString()
  @IsNotEmpty({ message: '密码不能为空' })
  @MinLength(6, { message: '密码长度不能少于6位' })
  password: string;

  @IsString()
  @IsIn(['student', 'teacher', 'admin'], { message: '角色必须为 student、teacher 或 admin' })
  role: 'student' | 'teacher' | 'admin';
}
