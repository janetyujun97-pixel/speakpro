# SpeakPro 开发指引

## 项目概述

SpeakPro 是 AI 驱动的托福/雅思口语练习平台，包含三个子项目。

## 架构要点

- **后端双服务**：NestJS (端口 3000) 处理 CRUD；Go (端口 8080) 处理音频/WebSocket/AI 编排
- **Nginx 路由分发**：`/api/v1/practice/audio`, `/api/v1/conversation/*`, `/api/v1/assessment/*`, `/api/v1/tts/*` → Go；其余 → NestJS
- **JWT 共享**：两个后端服务共用同一 JWT_SECRET
- **数据库迁移**：统一使用 `server/database/migrations/` 下的 SQL 文件，不使用 ORM 自动同步

## 开发规范

- 默认使用**简体中文**书写注释和文档
- NestJS 使用 TypeScript strict 模式
- Go 代码遵循 Go 官方代码规范
- Web 前端使用 kebab-case 文件命名
- iOS 使用 PascalCase 文件命名

## 常用命令

```bash
make dev-infra    # 启动 PG + Redis
make migrate      # 数据库迁移
make dev-nest     # 启动 NestJS
make dev-go       # 启动 Go 服务
make dev-web      # 启动 Web 前端
```
