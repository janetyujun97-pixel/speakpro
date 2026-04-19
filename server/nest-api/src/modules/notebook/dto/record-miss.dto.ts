import {
  IsArray,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

/**
 * Go orchestrator 跑完 ISE 评测后对每个低分单词调一次（或批量）。
 * 鉴权走 `X-Internal-Secret` header + INTERNAL_SHARED_SECRET 环境变量。
 */
export class RecordMissItem {
  @IsString()
  @IsNotEmpty()
  word: string;

  @IsOptional()
  @IsString()
  ipa?: string;

  @IsNumber()
  @Min(0)
  @Max(100)
  score: number;

  @IsOptional()
  @IsString()
  note?: string;
}

export class RecordMissDto {
  @IsUUID()
  userId: string;

  @IsOptional()
  @IsUUID()
  sessionId?: string;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => RecordMissItem)
  items: RecordMissItem[];
}
