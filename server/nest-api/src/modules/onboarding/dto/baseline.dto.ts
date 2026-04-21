import {
  IsNotEmpty,
  IsOptional,
  IsString,
  IsUUID,
} from 'class-validator';

/**
 * 录完基线音频后 iOS/Android 调 Go 拿 session_id，然后把 session_id 报给 NestJS。
 * 或者 client 直接传 audioUrl + 转写，Nest 端不生成 session，仅存到 onboarding_profiles。
 * 当前版本两种字段都接受，优先用 sessionId。
 */
export class BaselineDto {
  @IsOptional()
  @IsUUID()
  sessionId?: string;

  @IsOptional()
  @IsString()
  audioUrl?: string;

  @IsOptional()
  @IsString()
  transcript?: string;
}
