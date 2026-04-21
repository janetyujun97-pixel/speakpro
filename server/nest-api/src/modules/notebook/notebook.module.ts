import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { NotebookPhrase } from './entities/notebook-phrase.entity';
import { NotebookWord } from './entities/notebook-word.entity';
import { NotebookController } from './notebook.controller';
import { NotebookInternalController } from './notebook-internal.controller';
import { NotebookService } from './notebook.service';

@Module({
  imports: [TypeOrmModule.forFeature([NotebookWord, NotebookPhrase])],
  controllers: [NotebookController, NotebookInternalController],
  providers: [NotebookService],
  exports: [NotebookService],
})
export class NotebookModule {}
