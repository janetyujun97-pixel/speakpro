import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 20 })
  role: 'student' | 'teacher' | 'admin';

  @Column({ type: 'varchar', length: 20, unique: true, nullable: true })
  phone: string;

  @Column({ type: 'varchar', length: 255, unique: true, nullable: true })
  email: string;

  @Column({ type: 'varchar', length: 255, select: false })
  password: string;

  @Column({ type: 'varchar', length: 100 })
  name: string;

  @Column({ type: 'text', nullable: true, name: 'avatar_url' })
  avatarUrl: string;

  @Column({ type: 'varchar', length: 20, default: 'tencent', name: 'asr_provider' })
  asrProvider: string;

  @Column({ type: 'varchar', length: 20, default: 'tencent', name: 'ise_provider' })
  iseProvider: string;

  @Column({ type: 'varchar', length: 20, default: 'mimo', name: 'llm_provider' })
  llmProvider: string;

  @Column({ type: 'varchar', length: 20, default: 'mimo', name: 'tts_provider' })
  ttsProvider: string;

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ type: 'timestamptz', name: 'updated_at' })
  updatedAt: Date;
}
