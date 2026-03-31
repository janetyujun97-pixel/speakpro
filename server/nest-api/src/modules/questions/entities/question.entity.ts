import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('questions')
export class Question {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 10, name: 'exam_type' })
  examType: string;

  @Column({ type: 'varchar', length: 50 })
  section: string;

  @Column({ type: 'varchar', length: 200, nullable: true })
  topic: string;

  @Column({ type: 'text', name: 'prompt_text' })
  promptText: string;

  @Column({ type: 'text', nullable: true, name: 'sample_audio_url' })
  sampleAudioUrl: string;

  @Column({ type: 'text', nullable: true, name: 'sample_text' })
  sampleText: string;

  @Column({ type: 'int', nullable: true })
  difficulty: number;

  @Column({ type: 'jsonb', default: '[]' })
  tags: string[];

  @Column({ type: 'uuid', nullable: true, name: 'created_by' })
  createdBy: string;

  @ManyToOne(() => User, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'created_by' })
  creator: User;

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ type: 'timestamptz', name: 'updated_at' })
  updatedAt: Date;
}
