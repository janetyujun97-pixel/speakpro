import { IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class AppleSigninDto {
  @IsString()
  @IsNotEmpty({ message: 'identityToken 不能为空' })
  identityToken: string;

  @IsOptional()
  @IsString()
  authorizationCode?: string;

  @IsOptional()
  @IsString()
  nonce?: string;

  // 首次登录时 Apple 才返回 fullName，后续登录拿不到，客户端需要缓存
  @IsOptional()
  @IsString()
  name?: string;
}
