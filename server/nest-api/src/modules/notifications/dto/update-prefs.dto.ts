import { IsBoolean, IsOptional, Matches } from 'class-validator';

// HH:MM 或 HH:MM:SS
const TIME_RE = /^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/;

export class UpdatePrefsDto {
  @IsOptional()
  @Matches(TIME_RE, { message: 'quietStart 格式应为 HH:MM 或 HH:MM:SS' })
  quietStart?: string;

  @IsOptional()
  @Matches(TIME_RE, { message: 'quietEnd 格式应为 HH:MM 或 HH:MM:SS' })
  quietEnd?: string;

  @IsOptional()
  @IsBoolean()
  pushEnabled?: boolean;
}
