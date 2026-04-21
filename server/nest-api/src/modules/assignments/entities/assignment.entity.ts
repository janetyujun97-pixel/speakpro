import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
  OneToMany,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Submission } from './submission.entity';

@Entity('assignments')
export class Assignment {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 200 })
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'uuid', name: 'class_id' })
  classId: string;

  @Column({ type: 'uuid', nullable: true, name: 'teacher_id' })
  teacherId: string;

  @ManyToOne(() => User, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'teacher_id' })
  teacher: User;

  @Column({ type: 'uuid', array: true, name: 'question_ids' })
  questionIds: string[];

  @Column({ type: 'timestamptz', nullable: true, name: 'due_date' })
  dueDate: Date;

  /** 老师为整个作业录制的语音备注（PR3a 引入），OSS 上传后写入 */
  @Column({ type: 'text', nullable: true, name: 'teacher_voice_url' })
  teacherVoiceUrl: string | null;

  @OneToMany(() => Submission, (submission) => submission.assignment)
  submissions: Submission[];

  @CreateDateColumn({ type: 'timestamptz', name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ type: 'timestamptz', name: 'updated_at' })
  updatedAt: Date;
}
