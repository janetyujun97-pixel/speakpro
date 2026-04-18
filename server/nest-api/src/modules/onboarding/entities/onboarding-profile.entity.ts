import {
  Column,
  Entity,
  JoinColumn,
  OneToOne,
  PrimaryColumn,
  UpdateDateColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export type ExamType = 'IELTS' | 'TOEFL' | 'GENERAL';

@Entity('onboarding_profiles')
export class OnboardingProfile {
  @PrimaryColumn({ type: 'uuid', name: 'user_id' })
  userId: string;

  @OneToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'varchar', length: 20, nullable: true, name: 'exam_type' })
  examType: ExamType | null;

  @Column({
    type: 'numeric',
    precision: 3,
    scale: 1,
    nullable: true,
    name: 'target_score',
    transformer: {
      to: (v?: number | null) => v,
      from: (v?: string | null) => (v == null ? null : parseFloat(v)),
    },
  })
  targetScore: number | null;

  @Column({ type: 'date', nullable: true, name: 'exam_date' })
  examDate: string | null;

  @Column({ type: 'smallint', nullable: true, name: 'self_level' })
  selfLevel: number | null;

  @Column({ type: 'uuid', nullable: true, name: 'baseline_session_id' })
  baselineSessionId: string | null;

  @Column({ type: 'jsonb', nullable: true, name: 'study_plan' })
  studyPlan: Record<string, any> | null;

  @Column({ type: 'timestamptz', nullable: true, name: 'completed_at' })
  completedAt: Date | null;

  @UpdateDateColumn({ type: 'timestamptz', name: 'updated_at' })
  updatedAt: Date;
}
