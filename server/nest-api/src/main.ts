import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import helmet from 'helmet';
import { AppModule } from './app.module';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';
import { TransformInterceptor } from './common/interceptors/transform.interceptor';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  app.setGlobalPrefix('api/v1');

  // 安全头
  app.use(helmet());

  // CORS — 限制来源白名单
  const allowedOrigins = configService
    .get<string>('CORS_ORIGINS', 'http://localhost:3001,http://localhost:8888')
    .split(',')
    .map((s) => s.trim());

  app.enableCors({
    origin: allowedOrigins,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  });

  // 全局管道 & 过滤器
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.useGlobalFilters(new HttpExceptionFilter());
  app.useGlobalInterceptors(new TransformInterceptor());

  const port = configService.get<number>('NEST_PORT', 3000);
  await app.listen(port);
  console.log(`NestJS 服务已启动: http://localhost:${port}/api/v1`);
}
bootstrap();
