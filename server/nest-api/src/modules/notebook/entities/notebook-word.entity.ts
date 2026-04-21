import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  PrimaryGeneratedColumn,
  Unique,
} from 'typeorm';

@Entity('notebook_words')
@Unique(['userId', 'word'])
@Index(['userId', 'nextReviewAt'])
export class NotebookWord {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', name: 'user_id' })
  userId: string;

  @Column({ type: 'varchar', length: 64 })
  word: string;

  @Column({ type: 'varchar', length: 64, nullable: true })
  ipa: string | null;

  @Column({ type: 'text', nullable: true })
  note: string | null;

  @Column({ type: 'uuid', nullable: true, name: 'source_session_id' })
  sourceSessionId: string | null;

  @Column({ type: 'int', name: 'miss_count', default: 1 })
  missCount: number;

  @Column({ type: 'timestamptz', nullable: true, name: 'last_seen_at' })
  lastSeenAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true, name: 'mastered_at' })
  masteredAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true, name: 'next_review_at' })
  nextReviewAt: Date | null;

  @Column({
    type: 'numeric',
    precision: 3,
    scale: 2,
    default: 2.5,
    transformer: {
      to: (v?: number | null) => v,
      from: (v?: string | null) => (v == null ? 2.5 : parseFloat(v)),
    },
  })
  ef: number;

  @Column({ type: 'int', default: 0, name: 'interval_days' })
  intervalDays: number;

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;
}
