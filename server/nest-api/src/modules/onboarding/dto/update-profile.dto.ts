import {
  IsDateString,
  IsIn,
  IsInt,
  IsNumber,
  IsOptional,
  Max,
  Min,
} from 'class-validator';

export class UpdateProfileDto {
  @IsOptional()
  @IsIn(['IELTS', 'TOEFL', 'GENERAL'])
  examType?: 'IELTS' | 'TOEFL' | 'GENERAL';

  // IELTS 4.0–9.0 或 TOEFL 0–30；一个字段承载两种考试，服务端做一次范围校验
  @IsOptional()
  @IsNumber({ maxDecimalPlaces: 1 }, { message: 'target_score 最多 1 位小数' })
  @Min(0)
  @Max(30)
  targetScore?: number;

  @IsOptional()
  @IsDateString({}, { message: 'examDate 必须是 ISO 日期字符串' })
  examDate?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(5)
  selfLevel?: number;
}
