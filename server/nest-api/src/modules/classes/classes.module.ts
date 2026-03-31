import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClassesController } from './classes.controller';
import { ClassesService } from './classes.service';
import { Class } from './entities/class.entity';
import { User } from '../users/entities/user.entity';
import { PracticeSession } from '../practice/entities/practice-session.entity';
import { Submission } from '../assignments/entities/submission.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Class, User, PracticeSession, Submission])],
  controllers: [ClassesController],
  providers: [ClassesService],
  exports: [ClassesService],
})
export class ClassesModule {}
