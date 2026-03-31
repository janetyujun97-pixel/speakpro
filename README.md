# SpeakPro

AI 驱动的托福/雅思口语智能练习平台。

## 项目结构

```
speakpro/
├── server/          # 后端服务 (Go + NestJS)
│   ├── go-services/ # Go 服务 - 音频流处理、WebSocket、AI 编排
│   ├── nest-api/    # NestJS 服务 - CRUD 业务、认证、资源管理
│   ├── database/    # 数据库迁移文件
│   └── nginx/       # API 网关配置
├── web/             # Web 教师后台 (Next.js 14)
├── ios/             # iOS 学生端 (Swift/SwiftUI)
└── .github/         # CI/CD 配置
```

## 技术栈

| 层级 | 技术 |
|------|------|
| iOS 客户端 | Swift 5.9 + SwiftUI + AVFoundation |
| Web 前端 | Next.js 14 + React 18 + TailwindCSS + Radix UI |
| 后端 (CRUD) | NestJS + TypeScript + PostgreSQL |
| 后端 (实时) | Go + Gin + WebSocket |
| 缓存 | Redis 7 |
| AI 服务 | 讯飞语音评测/ASR/TTS + 通义千问 |
| 部署 | Docker + Nginx |

## 快速开始

### 环境要求

- Node.js >= 20
- Go >= 1.22
- Docker & Docker Compose
- Xcode 15+ (iOS 开发)

### 启动后端

```bash
# 1. 复制环境变量
cp server/.env.example server/.env

# 2. 启动基础设施 (PostgreSQL + Redis)
make dev-infra

# 3. 执行数据库迁移
make migrate

# 4. 启动 NestJS 服务
make dev-nest

# 5. 启动 Go 服务
make dev-go
```

### 启动 Web 教师后台

```bash
cd web
npm install
npm run dev
```

### iOS 开发

在 Xcode 中打开 `ios/SpeakPro/SpeakPro.xcodeproj`。
