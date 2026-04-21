import {
  Column,
  Entity,
  PrimaryColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('user_notification_prefs')
export class UserNotificationPrefs {
  @PrimaryColumn({ type: 'uuid', name: 'user_id' })
  userId: string;

  /** 免打扰开始时间，HH:MM:SS 字符串（Postgres TIME） */
  @Column({ type: 'time', name: 'quiet_start', default: '22:30' })
  quietStart: string;

  @Column({ type: 'time', name: 'quiet_end', default: '07:30' })
  quietEnd: string;

  @Column({ type: 'boolean', name: 'push_enabled', default: true })
  pushEnabled: boolean;

  @UpdateDateColumn({ type: 'timestamptz', name: 'updated_at' })
  updatedAt: Date;
}
