# SpeakPro 项目架构说明文档

## 一、项目概述

SpeakPro 是一个 **AI 驱动的托福/雅思口语练习平台**，包含三端客户端和两个后端服务，覆盖完整的口语练习、评测、批改、数据分析全流程。

| 指标 | 数据 |
|------|------|
| 源代码文件 | 225+ 个 |
| 编程语言 | Kotlin (49) + Swift (53) + TypeScript (129) + Go (19) |
| Git 提交 | 19 次 |
| 开发周期 | Phase 1-6 全部完成 |

---

## 二、系统架构

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  iOS 客户端  │  │Android 客户端│  │ Web 教师后台 │
│  (SwiftUI)  │  │(Compose)    │  │ (Next.js)   │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       │     HTTP/WebSocket (JSON)       │
       │                │                │
┌──────┴────────────────┴────────────────┴──────┐
│                  Nginx 网关                     │
│         端口 80 — 路由分发                       │
├────────────────────┬─────────────────────────────┤
│                    │                             │
│  ┌─────────────┐   │   ┌──────────────────┐      │
│  │ NestJS API  │   │   │  Go AI 服务      │      │
│  │ (端口 3000) │   │   │  (端口 8081)     │      │
│  │             │   │   │                  │      │
│  │ • 认证 JWT  │   │   │ • WebSocket 对话 │      │
│  │ • 用户 CRUD │   │   │ • 讯飞 ASR/ISE  │      │
│  │ • 题库管理  │   │   │ • 千问 AI 评估  │      │
│  │ • 作业系统  │   │   │ • Fish Audio TTS│      │
│  │ • 班级管理  │   │   │ • 音频评测流水线 │      │
│  │ • 练习统计  │   │   │ • full-evaluate │      │
│  └──────┬──────┘   │   └────────┬─────────┘      │
│         │          │            │                 │
│  ┌──────┴──────────┴────────────┴──────┐          │
│  │          PostgreSQL 16              │          │
│  │          (端口 5432)                │          │
│  └─────────────────────────────────────┘          │
│  ┌─────────────────────────────────────┐          │
│  │          Redis 7                    │          │
│  │          (端口 6380)                │          │
│  └─────────────────────────────────────┘          │
└───────────────────────────────────────────────────┘
```

### Nginx 路由规则

| 请求路径 | 转发目标 |
|---------|---------|
| `/api/v1/practice/audio` | Go (8081) |
| `/api/v1/conversation/*` | Go (8081) |
| `/api/v1/assessment/*` | Go (8081) |
| `/api/v1/tts/*` | Go (8081) |
| `/api/v1/*` (其余) | NestJS (3000) |

### JWT 共享

两个后端服务共用同一 `JWT_SECRET`，客户端只需一次登录即可访问所有服务。

---

## 三、项目目录结构

```
speakpro/
├── server/                          # 后端服务
│   ├── nest-api/                    # NestJS CRUD 服务
│   │   ├── src/
│   │   │   ├── modules/
│   │   │   │   ├── auth/            # 认证（注册/登录/刷新 Token）
│   │   │   │   ├── users/           # 用户管理 + 密码修改
│   │   │   │   ├── practice/        # 练习会话 + 统计 + 评分回写
│   │   │   │   ├── questions/       # 题库 CRUD + 批量导入
│   │   │   │   ├── assignments/     # 作业 + 提交 + 批改
│   │   │   │   ├── classes/         # 班级 + 学生管理 + 分析
│   │   │   │   └── resources/       # 资源上传（OSS 签名）
│   │   │   └── common/guards/       # JWT + 角色守卫
│   │   └── Dockerfile
│   │
│   ├── go-services/                 # Go AI 编排服务
│   │   ├── cmd/server/main.go       # 入口 + 路由注册
│   │   ├── internal/
│   │   │   ├── handler/             # HTTP/WebSocket 处理器
│   │   │   │   ├── conversation.go  # WebSocket AI 对话
│   │   │   │   ├── assessment.go    # 评测 + full-evaluate
│   │   │   │   ├── tts.go           # TTS 合成
│   │   │   │   └── audio.go         # 音频上传评测
│   │   │   ├── service/             # AI 服务封装
│   │   │   │   ├── xunfei.go        # 讯飞 ASR + ISE + TTS
│   │   │   │   ├── qwen.go          # 千问对话 + 语法 + 反馈 + 修订 + 思维导图 + 关键词
│   │   │   │   ├── fish_tts.go      # Fish Audio TTS（主 TTS 服务）
│   │   │   │   └── orchestrator.go  # 评测流水线编排（并行）
│   │   │   ├── model/               # 数据模型
│   │   │   ├── middleware/           # JWT 认证中间件
│   │   │   └── config/              # 环境配置
│   │   └── pkg/ws/                  # WebSocket Hub + Client
│   │
│   ├── database/
│   │   ├── migrations/              # SQL 迁移文件
│   │   └── seeds/                   # 种子数据
│   ├── nginx/nginx.conf             # Nginx 路由配置
│   ├── docker-compose.yml           # PG + Redis + Nginx
│   └── .env                         # 环境变量
│
├── ios/                             # iOS 客户端（SwiftUI）
│   ├── SpeakPro/
│   │   ├── App/                     # 入口 + 协调器（Tab 导航 + Auth）
│   │   ├── DesignSystem/            # 设计系统
│   │   │   ├── Colors.swift         # 色彩体系
│   │   │   ├── Typography.swift     # 字体层级
│   │   │   └── Components/          # ScoreRing, RadarChart, RecordButton, etc.
│   │   ├── Core/
│   │   │   ├── Network/             # APIClient + WebSocket + Endpoints
│   │   │   ├── Audio/               # AudioRecorder(16kHz PCM) + AudioPlayer
│   │   │   └── Storage/             # Keychain + LocalCache
│   │   ├── Features/
│   │   │   ├── Auth/                # 登录页
│   │   │   ├── Home/                # 首页（问候+进度+快速入口+推荐）
│   │   │   ├── Practice/            # 四种练习模式
│   │   │   │   ├── Conversation/    # AI 对话（WebSocket 实时交互）
│   │   │   │   ├── FollowRead/      # 跟读练习（逐句 TTS + ISE 评测）
│   │   │   │   ├── ReadAloud/       # 朗读练习（多段落 + 评测）
│   │   │   │   └── MockExam/        # 模考（full-evaluate + 4Tab 结果）
│   │   │   ├── Homework/            # 作业（列表+详情+题目展开）
│   │   │   ├── Progress/            # 进度（图表+雷达图）
│   │   │   └── Profile/             # 个人中心
│   │   └── Resources/               # 图标 + 动画资源
│   ├── project.yml                  # XcodeGen 配置
│   └── SpeakPro.xcodeproj/          # Xcode 工程
│
├── android/                         # Android 客户端（Jetpack Compose）
│   ├── app/src/main/java/com/speakpro/
│   │   ├── App.kt + MainActivity.kt # 入口
│   │   ├── navigation/              # 5 Tab 导航 + 路由
│   │   ├── designsystem/            # 设计系统（完全对标 iOS）
│   │   ├── core/
│   │   │   ├── network/             # Retrofit + OkHttp WS + Hilt DI
│   │   │   ├── audio/               # AudioRecord + MediaPlayer
│   │   │   └── storage/             # EncryptedSharedPreferences
│   │   ├── features/                # 功能模块（完全复刻 iOS）
│   │   └── data/models/             # API 数据模型
│   └── build.gradle.kts             # AGP 8.7 + Kotlin 2.0 + Compose
│
├── web/                             # Web 教师后台（Next.js 14）
│   ├── app/
│   │   ├── (auth)/login/            # 登录页
│   │   └── (dashboard)/             # 受保护的管理页面
│   │       ├── page.tsx             # Dashboard（KPI + 图表）
│   │       ├── resources/questions/ # 题库 CRUD
│   │       ├── resources/library/   # 资源库
│   │       ├── assignments/         # 作业管理 + 批改
│   │       ├── classes/             # 班级 + 学生花名册
│   │       └── settings/            # 设置
│   ├── components/
│   │   ├── ui/                      # 基础组件（Button, Card, Input）
│   │   ├── layout/                  # Sidebar + Header
│   │   ├── dashboard/              # 图表组件
│   │   └── grading/                # 批改组件（AudioPlayer, Transcript, ScoreAdjuster）
│   ├── lib/                         # API + Auth + Upload 工具
│   └── middleware.ts                # 路由鉴权
│
├── .github/workflows/               # CI/CD
│   ├── server-ci.yml                # NestJS + Go 构建测试
│   └── web-ci.yml                   # Next.js 构建
│
├── Makefile                         # 开发命令
├── .claude/launch.json              # 开发服务器配置
└── CLAUDE.md                        # 项目开发指引
```

---

## 四、技术栈

### 后端

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| CRUD API | NestJS + TypeScript | 10.x | 用户/题库/作业/班级/统计 |
| AI 服务 | Go + Gin | 1.26 | WebSocket/音频处理/AI 编排 |
| 数据库 | PostgreSQL | 16 | 主数据库（UUID 主键, JSONB） |
| 缓存 | Redis | 7 | 会话缓存 |
| ORM | TypeORM | 10.x | NestJS 数据访问 |
| 认证 | JWT (jsonwebtoken) | - | 双服务共享 secret |

### AI 服务集成

| 服务 | 提供商 | 用途 |
|------|--------|------|
| ASR 语音识别 | 讯飞 IAT | 实时语音转文字（WebSocket） |
| ISE 发音评测 | 讯飞 ISE | 发音/流利度/完整度/语调评分 |
| 对话/评估 | 通义千问 (qwen-max) | AI 考官对话 + 语法纠错 + 综合反馈 + 修订答案 + 思维导图 + 关键词 |
| TTS 语音合成 | Fish Audio (s2-pro) | 自然英文语音合成（MP3） |
| TTS 备选 | 讯飞 TTS | PCM 格式语音合成（回退方案） |

### iOS 客户端

| 组件 | 技术 | 说明 |
|------|------|------|
| UI | SwiftUI | 声明式 UI |
| 架构 | MVVM + Coordinator | 状态管理 + 导航 |
| 网络 | URLSession | async/await |
| WebSocket | URLSessionWebSocketTask | AI 对话实时通信 |
| 音频录制 | AVAudioEngine + AVAudioConverter | 48kHz→16kHz 重采样 |
| 音频播放 | AVAudioPlayer | MP3/WAV/PCM 支持 |
| 存储 | Keychain + UserDefaults | Token + 用户信息 |
| 最低版本 | iOS 17.0 | - |

### Android 客户端

| 组件 | 技术 | 说明 |
|------|------|------|
| UI | Jetpack Compose | Material3 |
| 架构 | MVVM + Hilt DI | ViewModel + StateFlow |
| 网络 | Retrofit + OkHttp | Gson 序列化 |
| WebSocket | OkHttp WebSocket | AI 对话实时通信 |
| 音频录制 | AudioRecord | 直接 16kHz PCM（无需重采样） |
| 音频播放 | MediaPlayer | MP3/WAV 支持 |
| 存储 | EncryptedSharedPreferences | 加密 Token 存储 |
| 最低版本 | Android 8.0 (API 26) | - |

### Web 教师后台

| 组件 | 技术 | 说明 |
|------|------|------|
| 框架 | Next.js 14 (App Router) | SSR + CSR |
| UI | React 18 + TailwindCSS | Radix UI 基础组件 |
| 图表 | Recharts | 折线图 + 雷达图 + 柱状图 |
| 表单 | React Hook Form + Zod | 表单验证 |
| 状态 | React Query | 数据缓存 |

---

## 五、核心功能模块

### 1. AI 对话练习（WebSocket 实时交互）

```
用户录音 → [音频流 base64] → Go WebSocket
         → 讯飞 ASR 转写 → 文本
         → 讯飞 ISE 评测 → 发音/流利度评分
         → 千问 AI 对话 → 考官追问
         → Fish Audio TTS → 考官语音
         → [评分+转写+语音] ← 返回客户端
```

### 2. 模考完整评测（full-evaluate 聚合 API）

```
POST /assessment/full-evaluate

Step 1 (串行): 讯飞 ASR → transcript
Step 2 (6 个并行 goroutine):
  A: 讯飞 ISE → 发音评分
  B: 千问 → 语法纠错
  C: 千问 → 综合反馈
  D: 千问 → 修订答案
  E: 千问 → 思维导图
  F: 千问 → 关键词 + 样例答案
Step 3 (串行): Fish Audio TTS → 修订答案语音

返回: transcript + 5 维评分 + 修订 + 导图 + 关键词 + 样例 + TTS 音频
```

### 3. 跟读/朗读练习

```
Fish Audio TTS 生成参考音 → 用户跟读录音 → 讯飞 ISE 评测
→ 发音/语调/流利度评分 → 波形对比 → AI 改进建议
```

### 4. 教师后台（Web）

```
题库管理(CRUD) → 作业创建(选题+分配班级) → 学生提交
→ 教师批改(音频回放+转写+评分调整) → 数据看板(趋势+排行)
```

---

## 六、数据库 Schema

```sql
users          -- 用户（学生/教师/管理员）
classes        -- 班级
class_students -- 班级-学生关联（多对多）
questions      -- 题库（JSONB tags, difficulty 1-5）
practice_sessions -- 练习会话（JSONB 评分字段）
assignments    -- 作业（UUID[] questionIds）
submissions    -- 作业提交（sessionIds, 教师评分）
resources      -- 教学资源（文件元数据）
```

---

## 七、API 端点汇总

### NestJS (端口 3000)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录 |
| POST | `/auth/register` | 注册 |
| POST | `/auth/refresh` | 刷新 Token |
| GET | `/users/profile` | 获取个人信息 |
| PUT | `/users/profile` | 更新个人信息 |
| PUT | `/users/password` | 修改密码 |
| GET | `/users?role=student` | 按角色查用户 |
| POST | `/practice/start` | 开始练习 |
| GET | `/practice/stats` | 练习统计 |
| GET | `/practice/sessions` | 练习历史 |
| POST | `/practice/sessions/batch` | 批量查询会话 |
| PATCH | `/practice/sessions/:id/scores` | 评分回写（Go→NestJS） |
| GET | `/questions` | 题目列表（分页+筛选） |
| POST | `/questions` | 创建题目 |
| PUT | `/questions/:id` | 更新题目 |
| DELETE | `/questions/:id` | 删除题目 |
| POST | `/questions/import` | 批量导入 |
| GET | `/assignments` | 作业列表 |
| POST | `/assignments` | 创建作业 |
| POST | `/assignments/:id/submit` | 学生提交 |
| PUT | `/assignments/:id/grade` | 教师批改 |
| POST | `/classes` | 创建班级 |
| PUT | `/classes/:id` | 编辑班级 |
| DELETE | `/classes/:id` | 删除班级 |
| POST | `/classes/:id/students` | 添加学生 |
| DELETE | `/classes/:id/students/:sid` | 移除学生 |
| GET | `/classes/:id/analytics` | 班级分析 |
| GET | `/classes/:id/score-trends` | 成绩趋势 |
| GET | `/classes/:id/leaderboard` | 学生排行 |

### Go AI 服务 (端口 8081)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| WS | `/conversation/ws/:sessionId` | AI 对话 WebSocket |
| POST | `/practice/audio` | 音频上传评测 |
| POST | `/assessment/evaluate` | 发音评测 |
| POST | `/assessment/full-evaluate` | 完整 AI 评测（模考用） |
| POST | `/assessment/feedback` | AI 综合反馈 |
| POST | `/tts/synthesize` | TTS 语音合成 |

---

## 八、环境变量

```env
# 数据库
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

# Redis
REDIS_HOST, REDIS_PORT

# JWT
JWT_SECRET, JWT_EXPIRES_IN

# 服务端口
NEST_PORT=3000, GO_PORT=8081

# 讯飞
XUNFEI_APP_ID, XUNFEI_API_KEY, XUNFEI_API_SECRET

# 千问
QWEN_API_KEY, QWEN_MODEL=qwen-max

# Fish Audio TTS
FISH_AUDIO_API_KEY, FISH_AUDIO_MODEL=s2-pro

# 阿里云 OSS
OSS_REGION, OSS_BUCKET, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET
```

---

## 九、开发命令

```bash
# 基础设施
make dev-infra          # 启动 PostgreSQL + Redis
make migrate            # 数据库迁移

# 后端
make dev-nest           # NestJS 开发服务器 (端口 3000)
make dev-go             # Go AI 服务 (端口 8081)

# 前端
make dev-web            # Web 教师后台 (端口 3001)

# iOS
cd ios && xcodegen generate && xcodebuild ...

# Android
cd android && ./gradlew assembleDebug
adb reverse tcp:3000 tcp:3000   # USB 端口转发
adb reverse tcp:8081 tcp:8081
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 十、测试账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 教师 | teacher@speakpro.com | teacher123 |
| 学生 | student1@speakpro.com | student123 |
| 管理员 | admin@speakpro.com | teacher123 |

---

## 十一、设计系统

### 色彩

| 名称 | 色值 | 用途 |
|------|------|------|
| Primary | #1B2A4A | 主色（深靛蓝） |
| Accent | #FF6B4A | 强调色（珊瑚橙） |
| Background | #F8F7F4 | 背景色（暖灰白） |
| Success | #2DD4A8 | 成功（薄荷绿） |
| Warning | #F59E0B | 警告（琥珀色） |
| Error | #EF4444 | 错误（红色） |

### 字体层级

| 级别 | 大小 | 权重 |
|------|------|------|
| TitleLarge | 28sp | Bold |
| TitleMedium | 22sp | SemiBold |
| TitleSmall | 18sp | SemiBold |
| BodyLarge | 17sp | Regular |
| BodyMedium | 15sp | Regular |
| BodySmall | 13sp | Regular |
| Caption | 11sp | Regular |

### 共享组件

| 组件 | 说明 |
|------|------|
| ScoreRing | 环形进度评分（Canvas 动画） |
| RadarChart | 五维雷达图 |
| RecordButton | 脉动录音按钮 |
| WaveformView | 音频波形可视化（自动归一化） |
| PracticeCard | 练习模式入口卡片 |
| RichFeedbackView | AI 反馈富文本渲染（Markdown 解析） |
