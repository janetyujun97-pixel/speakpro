import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Question } from '../../questions/entities/question.entity';

@Entity('practice_sessions')
export class PracticeSession {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', name: 'student_id' })
  studentId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'student_id' })
  student: User;

  @Column({ type: 'uuid', nullable: true, name: 'question_id' })
  questionId: string;

  @ManyToOne(() => Question, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'question_id' })
  question: Question;

  @Column({ type: 'varchar', length: 20 })
  mode: string;

  @Column({ type: 'text', nullable: true, name: 'audio_url' })
  audioUrl: string;

  @Column({ type: 'text', nullable: true })
  transcript: string;

  @Column({ type: 'int', nullable: true, name: 'duration_sec' })
  durationSec: number;

  @Column({ type: 'jsonb', nullable: true, name: 'pronunciation_score' })
  pronunciationScore: Record<string, any>;

  @Column({ type: 'jsonb', nullable: true, name: 'fluency_score' })
  fluencyScore: Record<string, any>;

  @Column({ type: 'jsonb', nullable: true, name: 'grammar_score' })
  grammarScore: Record<string, any>;

  @Column({ type: 'jsonb', nullable: true, name: 'content_score' })
  contentScore: Record<string, any>;

  @Column({ type: 'decimal', precision: 5, scale: 2, nullable: true, name: 'overall_score' })
  overallScore: number;

  @Column({ type: 'text', nullable: true, name: 'ai_feedback' })
  aiFeedback: string;

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;
}
