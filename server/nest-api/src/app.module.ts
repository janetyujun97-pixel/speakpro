import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { RedisModule } from './common/redis/redis.module';
import { AuthModule } from './modules/auth/auth.module';
import { UsersModule } from './modules/users/users.module';
import { QuestionsModule } from './modules/questions/questions.module';
import { PracticeModule } from './modules/practice/practice.module';
import { AssignmentsModule } from './modules/assignments/assignments.module';
import { ClassesModule } from './modules/classes/classes.module';
import { ResourcesModule } from './modules/resources/resources.module';
import { AnalyticsModule } from './modules/analytics/analytics.module';
import { OnboardingModule } from './modules/onboarding/onboarding.module';
import { NotebookModule } from './modules/notebook/notebook.module';
import { NotificationsModule } from './modules/notifications/notifications.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, envFilePath: '../.env' }),
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DB_HOST', 'localhost'),
        port: config.get<number>('DB_PORT', 5432),
        username: config.get('DB_USER', 'speakpro'),
        password: config.get('DB_PASSWORD', 'speakpro_dev_123'),
        database: config.get('DB_NAME', 'speakpro'),
        autoLoadEntities: true,
        synchronize: false,
      }),
    }),
    // 全局速率限制：每分钟最多 60 次请求
    ThrottlerModule.forRoot([{
      ttl: 60000,
      limit: 60,
    }]),
    RedisModule,
    AuthModule,
    UsersModule,
    QuestionsModule,
    PracticeModule,
    AssignmentsModule,
    ClassesModule,
    ResourcesModule,
    AnalyticsModule,
    OnboardingModule,
    NotebookModule,
    NotificationsModule,
  ],
  providers: [
    // 全局启用速率限制
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
