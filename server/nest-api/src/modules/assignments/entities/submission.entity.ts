import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Assignment } from './assignment.entity';

@Entity('submissions')
export class Submission {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', name: 'assignment_id' })
  assignmentId: string;

  @ManyToOne(() => Assignment, (assignment) => assignment.submissions, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'assignment_id' })
  assignment: Assignment;

  @Column({ type: 'uuid', name: 'student_id' })
  studentId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'student_id' })
  student: User;

  @Column({ type: 'uuid', array: true, nullable: true, name: 'session_ids' })
  sessionIds: string[];

  @Column({ type: 'varchar', length: 20, default: 'pending' })
  status: 'pending' | 'submitted' | 'graded';

  @Column({ type: 'text', nullable: true, name: 'teacher_comment' })
  teacherComment: string;

  @Column({ type: 'decimal', precision: 5, scale: 2, nullable: true, name: 'teacher_score' })
  teacherScore: number;

  @Column({ type: 'timestamptz', nullable: true, name: 'submitted_at' })
  submittedAt: Date;

  @Column({ type: 'timestamptz', nullable: true, name: 'graded_at' })
  gradedAt: Date;
}
