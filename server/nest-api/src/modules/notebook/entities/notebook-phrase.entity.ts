import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  PrimaryGeneratedColumn,
} from 'typeorm';

@Entity('notebook_phrases')
@Index(['userId'])
export class NotebookPhrase {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', name: 'user_id' })
  userId: string;

  @Column({ type: 'text' })
  phrase: string;

  @Column({ type: 'varchar', length: 64, nullable: true })
  note: string | null;

  @Column({ type: 'int', name: 'use_count', default: 1 })
  useCount: number;

  @Column({ type: 'timestamptz', nullable: true, name: 'last_seen_at' })
  lastSeenAt: Date | null;

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;
}
