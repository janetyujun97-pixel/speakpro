# SpeakPro — 托福雅思口语智能练习平台

## 开发方案书 v1.0

---

## 一、产品概述

**SpeakPro** 是一款面向托福/雅思考生的 AI 口语练习平台，核心价值是通过中国大模型驱动的 AI 对话 + 发音评估 + 教师督学，形成"练 → 评 → 纠 → 复"的完整口语训练闭环。

### 1.1 用户角色

| 角色 | 端 | 核心诉求 |
|------|-----|---------|
| 学生 | iOS App | 随时练口语、即时看评分、跟读纠音、模考演练 |
| 教师 | Web 后台 | 上传资源、布置/批改作业、查看学生练习数据 |
| 管理员 | Web 后台 | 账号管理、课程管理、数据统计 |

### 1.2 核心功能矩阵

**学生端 (iOS)：**
- AI 模拟对话（支持托福 Independent/Integrated、雅思 Part 1/2/3 全场景）
- 发音评估与纠音（音素级评分 + 重音/语调/流利度）
- 跟读模式（原声示范 → 学生跟读 → 对比评分）
- 朗读模式（TTS 示范 + 录音评分）
- 模考计时练习
- 作业系统（接收教师作业 → 完成 → 提交）
- 练习报告与成长曲线

**教师端 (Web)：**
- 资源管理（题库、音频、范文上传）
- 作业管理（创建、分发、批改）
- 学生数据看板（练习时长、评分趋势、弱项分析）
- 班级管理

---

## 二、系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端层                                  │
│  ┌──────────────────┐          ┌──────────────────────────┐     │
│  │   iOS App        │          │   Web 教师后台             │     │
│  │   Swift/UIKit    │          │   Next.js 14 + React     │     │
│  │   + SwiftUI      │          │   + TailwindCSS          │     │
│  └────────┬─────────┘          └────────────┬─────────────┘     │
└───────────┼──────────────────────────────────┼──────────────────┘
            │  HTTPS / WebSocket               │  HTTPS
            ▼                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       API 网关层 (Nginx + Kong)                   │
│            JWT 认证 · 限流 · 负载均衡 · SSL 终结                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                       业务服务层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐   │
│  │ 用户服务  │  │ 练习服务  │  │ 作业服务  │  │  资源服务      │   │
│  │ Auth     │  │ Practice │  │ Homework │  │  Resource     │   │
│  │ Profile  │  │ Session  │  │ Assign   │  │  Upload/CDN   │   │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────────────┐   │
│  │ 评分服务  │  │ 数据服务  │  │  AI 编排服务 (Orchestrator)   │   │
│  │ Score    │  │ Analytics│  │  对话管理 · Prompt 编排       │   │
│  │ Report   │  │ Dashboard│  │  评分聚合 · TTS 调度          │   │
│  └──────────┘  └──────────┘  └──────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                    AI 能力层（中国大模型）                          │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐     │
│  │ 讯飞开放平台   │  │ 阿里通义千问     │  │  百度文心一言     │     │
│  │ · 语音评测    │  │ · Qwen 对话     │  │  · 备用对话引擎   │     │
│  │ · 语音识别    │  │ · 口语纠错      │  │  · 文本生成      │     │
│  │ · 语音合成    │  │ · 评分生成      │  │                  │     │
│  └──────────────┘  └────────────────┘  └──────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                       数据存储层                                  │
│  ┌───────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ PostgreSQL│  │  Redis   │  │  阿里 OSS │  │ ElasticSearch│   │
│  │ 业务主库   │  │ 缓存/会话 │  │ 音频/文件 │  │ 题库全文检索  │   │
│  └───────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型一览

| 层级 | 技术 | 选型理由 |
|------|------|---------|
| iOS 客户端 | Swift 5.9 + SwiftUI + UIKit 混编 | SwiftUI 构建主 UI，UIKit 处理音频录制等复杂交互 |
| iOS 音频 | AVFoundation + AudioToolbox | 原生音频录制、播放、波形可视化 |
| iOS 网络 | URLSession + Combine | 原生异步网络 + 响应式数据流 |
| Web 前端 | Next.js 14 (App Router) + React 18 | SSR/SSG 支持、文件路由、React Server Components |
| Web UI | TailwindCSS + Radix UI + Framer Motion | 高效样式 + 无障碍组件 + 动效 |
| Web 图表 | Recharts + D3.js | 学生数据可视化 |
| 后端框架 | Go (Gin) / Node.js (NestJS) | Go 处理高并发音频流；Node 处理 CRUD 业务 |
| 实时通信 | WebSocket (Socket.IO) | AI 对话实时流式输出 |
| 数据库 | PostgreSQL 16 | JSONB 存评分详情、强事务支持 |
| 缓存 | Redis 7 | 会话缓存、对话上下文、限流 |
| 对象存储 | 阿里云 OSS | 音频文件、教学资源存储 |
| 搜索 | Elasticsearch 8 | 题库全文检索 |
| 部署 | 阿里云 ECS + Docker + K8s | 国内合规、与 AI 服务同区域低延迟 |
| CI/CD | GitHub Actions + 阿里云效 | 自动化构建与部署 |

### 2.3 AI 能力选型（中国大模型方案）

| 能力 | 推荐方案 | 备选 | 说明 |
|------|---------|------|------|
| **发音评估** | 讯飞语音评测 API | 腾讯智聆 | 支持音素级评分、重音/语调/完整度评估，专业口语评测能力最成熟 |
| **语音识别 (ASR)** | 讯飞实时语音识别 | 阿里智能语音 | 英文识别准确率高，支持流式识别 |
| **语音合成 (TTS)** | 讯飞语音合成 (英文) | 阿里语音合成 | 用于朗读示范，支持多种英文音色 |
| **AI 对话** | 通义千问 (qwen-max) | 百度文心 (ernie-4.0) | 用于模拟考官对话、口语纠错反馈、评分生成 |
| **文本纠错** | 通义千问 | 讯飞星火 | 对 ASR 转写文本进行语法纠错和建议 |

**关键设计：AI 编排服务**

后端设置一个 AI Orchestrator 服务，统一管理多个大模型 API 的调用编排：

```
学生说话 → 讯飞 ASR(实时转写) → 讯飞评测(发音评分)
                                       ↓
                              通义千问(语法纠错+评分生成)
                                       ↓
                              讯飞 TTS(示范发音回放)
                                       ↓
                              返回综合评估结果 → iOS 展示
```

---

## 三、数据库设计（核心表）

```sql
-- 用户表
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role        VARCHAR(20) NOT NULL CHECK (role IN ('student', 'teacher', 'admin')),
    phone       VARCHAR(20) UNIQUE,
    email       VARCHAR(255) UNIQUE,
    name        VARCHAR(100) NOT NULL,
    avatar_url  TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 班级表
CREATE TABLE classes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    teacher_id  UUID REFERENCES users(id),
    exam_type   VARCHAR(20) CHECK (exam_type IN ('TOEFL', 'IELTS', 'BOTH')),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 学生-班级关联
CREATE TABLE class_students (
    class_id    UUID REFERENCES classes(id),
    student_id  UUID REFERENCES users(id),
    joined_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (class_id, student_id)
);

-- 题库
CREATE TABLE questions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_type   VARCHAR(10) NOT NULL, -- TOEFL / IELTS
    section     VARCHAR(50) NOT NULL, -- e.g., 'Part1', 'Independent', 'Integrated'
    topic       VARCHAR(200),
    prompt_text TEXT NOT NULL,         -- 题目文本
    sample_audio_url TEXT,             -- 示范音频 OSS 地址
    sample_text TEXT,                  -- 示范答案文本
    difficulty  INT CHECK (difficulty BETWEEN 1 AND 5),
    tags        JSONB DEFAULT '[]',
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 练习记录
CREATE TABLE practice_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID REFERENCES users(id),
    question_id     UUID REFERENCES questions(id),
    mode            VARCHAR(20) NOT NULL, -- 'conversation', 'read_aloud', 'follow_read', 'mock_exam'
    audio_url       TEXT,                  -- 学生录音 OSS 地址
    transcript      TEXT,                  -- ASR 转写文本
    duration_sec    INT,
    -- 评分详情 (JSONB 存储讯飞返回 + AI 生成)
    pronunciation_score JSONB,  -- {overall: 85, phonemes: [...], stress: 78, intonation: 82}
    fluency_score   JSONB,      -- {score: 80, pace: 'normal', fillers: 3, pauses: [...]}
    grammar_score   JSONB,      -- {score: 75, errors: [...], corrections: [...]}
    content_score   JSONB,      -- {score: 88, relevance: 90, vocabulary: 85, coherence: 88}
    overall_score   DECIMAL(5,2),
    ai_feedback     TEXT,        -- AI 生成的综合反馈
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 作业表
CREATE TABLE assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    class_id    UUID REFERENCES classes(id),
    teacher_id  UUID REFERENCES users(id),
    question_ids UUID[] NOT NULL,       -- 包含的题目
    due_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 作业提交
CREATE TABLE submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID REFERENCES assignments(id),
    student_id      UUID REFERENCES users(id),
    session_ids     UUID[],              -- 关联的练习记录
    status          VARCHAR(20) DEFAULT 'pending', -- pending / submitted / graded
    teacher_comment TEXT,
    teacher_score   DECIMAL(5,2),
    submitted_at    TIMESTAMPTZ,
    graded_at       TIMESTAMPTZ
);

-- 教学资源
CREATE TABLE resources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    type        VARCHAR(20) NOT NULL, -- 'audio', 'document', 'video', 'wordlist'
    file_url    TEXT NOT NULL,
    file_size   BIGINT,
    exam_type   VARCHAR(10),
    tags        JSONB DEFAULT '[]',
    uploaded_by UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 四、iOS 学生端设计方案

### 4.1 信息架构

```
App
├── 首页 (Home)
│   ├── 今日推荐练习
│   ├── 待完成作业提醒
│   ├── 练习连续天数打卡
│   └── 快速入口卡片
│
├── 练习 (Practice)
│   ├── AI 对话练习
│   │   ├── 选择考试类型 (托福/雅思)
│   │   ├── 选择题型 (Part 1/2/3, Independent...)
│   │   ├── 对话界面 (实时语音交互)
│   │   └── 练习报告
│   │
│   ├── 跟读模式
│   │   ├── 示范播放 → 学生跟读 → 评分对比
│   │   └── 逐句打分 + 问题音素高亮
│   │
│   ├── 朗读模式
│   │   ├── 文本展示 + TTS 示范
│   │   └── 录音评分
│   │
│   └── 模考模式
│       ├── 计时模拟 (完整 Speaking Section)
│       └── 模考报告
│
├── 作业 (Homework)
│   ├── 待完成列表
│   ├── 已完成 / 已批改
│   └── 作业详情 & 练习入口
│
├── 成长 (Progress)
│   ├── 综合评分曲线
│   ├── 各维度雷达图
│   ├── 练习统计 (时长/次数/天数)
│   └── 弱项分析
│
└── 我的 (Profile)
    ├── 个人信息
    ├── 班级信息
    ├── 设置 (目标分数、每日提醒)
    └── 关于
```

### 4.2 核心页面设计方案

#### 页面 1：首页 (Home)

**设计理念：** 温暖鼓励 + 快速进入练习

- **顶部：** 问候语 + 连续打卡天数（火焰图标动效）
- **核心卡片区：**
  - "今日推荐" 大卡片：基于弱项推荐的练习题，滑动翻页
  - "待完成作业" 紧急提示条：红色角标 + 截止时间倒计时
- **快速入口：** 4 个圆角图标按钮 → AI 对话 / 跟读 / 朗读 / 模考
- **底部进度条：** 今日练习目标完成度（如 2/3 次练习）

**配色方向：**
- 主色：深靛蓝 (#1B2A4A) — 专业稳重
- 强调色：珊瑚橘 (#FF6B4A) — 活力鼓励
- 背景：暖灰白 (#F8F7F4) — 柔和不刺眼
- 成功色：薄荷绿 (#2DD4A8)

**字体：**
- 英文：SF Pro Display (系统) + Nunito (品牌字体，圆润友好)
- 中文：PingFang SC

---

#### 页面 2：AI 对话练习界面

**设计理念：** 沉浸式对话体验，像和真人考官对话

**布局结构（从上到下）：**

```
┌─────────────────────────────────┐
│  ← 返回    IELTS Part 2    ⏱ 2:00 │  ← 顶部导航 + 倒计时
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │  🎓 AI Examiner         │    │  ← 考官气泡（左对齐）
│  │  "Describe a place you  │    │     配考官虚拟头像
│  │   visited recently..."  │    │
│  └─────────────────────────┘    │
│                                 │
│        ┌─────────────────────┐  │
│        │  🎤 You             │  │  ← 学生气泡（右对齐）
│        │  实时转写文字显示...  │  │     说话时有脉冲波纹动效
│        │  ~~~~~~~~~~~~~~~~~~~│  │
│        └─────────────────────┘  │
│                                 │
│  ┌─ 实时反馈面板 ────────────┐   │
│  │ 🟢 Fluency   🟡 Grammar  │   │  ← 实时维度评分指示灯
│  │ 🟢 Vocabulary 🔴 Pronun. │   │
│  └───────────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│  ┌─────────────────────────┐    │
│  │   ◉◉◉ 声波可视化 ◉◉◉    │    │  ← 录音状态指示
│  └─────────────────────────┘    │
│      [结束本轮]    🔴 录音中     │  ← 底部操作区
└─────────────────────────────────┘
```

**交互细节：**
- 录音按钮：大圆按钮，按下时有脉冲光环动效
- 语音波形：实时音频振幅可视化（CoreAnimation 驱动）
- 考官回复：流式文字输出 + 可选 TTS 播放
- 实时转写：学生说话时文字实时出现在气泡中

---

#### 页面 3：跟读模式界面

**布局结构：**

```
┌─────────────────────────────────┐
│  ← 返回    Follow & Read   3/10  │
├─────────────────────────────────┤
│                                 │
│  " The phenomenon of global    │  ← 示范文本（大字）
│    warming has been a topic    │     当前播放句高亮
│    of significant debate. "    │     问题音素标红
│                                 │
│  ┌─ 波形对比 ─────────────────┐  │
│  │ 🔊 原声:  ▁▃▅▇▅▃▁▃▅▇▅▃▁  │  │  ← 双波形对比
│  │ 🎤 你的:  ▁▂▅▆▃▂▁▂▅▆▄▂▁  │  │     (Lottie动画)
│  └────────────────────────────┘  │
│                                 │
│  ┌─ 评分卡片 ─────────────────┐  │
│  │                             │  │
│  │  发音 92  语调 85  流利 88  │  │  ← 环形进度条
│  │                             │  │
│  │  问题音素：/θ/ → /s/       │  │  ← 具体纠音建议
│  │  "phenomenon" 中 th 发音    │  │
│  │  需要舌尖抵上齿...          │  │
│  └────────────────────────────┘  │
│                                 │
│  [▶ 再听一次]  [🎤 重新跟读]    │
│  [→ 下一句]                     │
└─────────────────────────────────┘
```

---

#### 页面 4：练习报告页

**设计理念：** 数据可视化 + 可操作的改进建议

```
┌─────────────────────────────────┐
│  ← 返回     练习报告    📤 分享  │
├─────────────────────────────────┤
│                                 │
│         ┌────────┐              │
│         │  88    │              │  ← 总分大数字
│         │ /100   │              │     环形进度条包裹
│         └────────┘              │
│       IELTS Part 2              │
│                                 │
│  ┌─ 维度评分 ─────────────────┐  │
│  │                             │  │
│  │    ╱ 发音 ╲                 │  │  ← 雷达图
│  │  流利      语法              │  │
│  │    ╲ 内容 ╱                 │  │
│  │                             │  │
│  └────────────────────────────┘  │
│                                 │
│  ┌─ AI 反馈 ──────────────────┐  │
│  │ ✅ 优点：话题展开充分，      │  │
│  │    使用了丰富的连接词...     │  │
│  │                             │  │
│  │ 💡 改进：注意 /θ/ 发音，     │  │
│  │    "think" 中 th 被发成...  │  │
│  │                             │  │
│  │ 📝 建议练习：               │  │
│  │    · 跟读训练 - th 发音专项  │  │
│  │    · 同类话题再练 3 次       │  │
│  └────────────────────────────┘  │
│                                 │
│  [🔄 再练一次]  [📋 加入复习单]  │
└─────────────────────────────────┘
```

---

#### 页面 5：作业详情页

```
┌─────────────────────────────────┐
│  ← 返回      作业详情           │
├─────────────────────────────────┤
│                                 │
│  📝 IELTS Speaking 专项训练      │
│  布置人：王老师                  │
│  截止：2026-04-05 23:59         │
│  ┌─────────────────────────┐    │
│  │ ⏰ 剩余 5 天 12 小时     │    │  ← 倒计时条
│  │ ████████░░░░░░░░░░░░░░  │    │
│  └─────────────────────────┘    │
│                                 │
│  题目列表 (3/5 已完成)           │
│  ┌─────────────────────────┐    │
│  │ ✅ Part 1: Hometown     │ 92 │  ← 已完成有分数
│  │ ✅ Part 1: Study        │ 85 │
│  │ ✅ Part 2: A Person     │ 78 │
│  │ ○  Part 2: An Event    │ →  │  ← 未完成可点击进入
│  │ ○  Part 3: Follow-up   │ →  │
│  └─────────────────────────┘    │
│                                 │
│  教师评语（批改后显示）           │
│  ┌─────────────────────────┐    │
│  │ "发音进步明显，Part 2 的 │    │
│  │  时间控制需要加强..."    │    │
│  └─────────────────────────┘    │
│                                 │
│  [📤 提交作业]                   │
└─────────────────────────────────┘
```

### 4.3 iOS 技术实现要点

**项目结构（Clean Architecture + MVVM）：**

```
SpeakPro/
├── App/
│   ├── SpeakProApp.swift
│   └── AppCoordinator.swift
├── Core/
│   ├── Network/
│   │   ├── APIClient.swift          -- URLSession + Combine 封装
│   │   ├── WebSocketManager.swift   -- 实时对话连接
│   │   └── Endpoints.swift
│   ├── Audio/
│   │   ├── AudioRecorder.swift      -- AVAudioEngine 录音
│   │   ├── AudioPlayer.swift        -- 音频播放
│   │   ├── WaveformGenerator.swift  -- 实时波形数据
│   │   └── AudioFileManager.swift   -- 本地音频缓存
│   ├── Storage/
│   │   ├── CoreDataStack.swift      -- 离线练习记录
│   │   └── KeychainManager.swift    -- Token 安全存储
│   └── Extensions/
├── Features/
│   ├── Home/
│   │   ├── HomeView.swift
│   │   └── HomeViewModel.swift
│   ├── Practice/
│   │   ├── Conversation/
│   │   │   ├── ConversationView.swift
│   │   │   ├── ConversationViewModel.swift
│   │   │   ├── ChatBubbleView.swift
│   │   │   └── WaveformView.swift
│   │   ├── FollowRead/
│   │   ├── ReadAloud/
│   │   └── MockExam/
│   ├── Homework/
│   ├── Progress/
│   └── Profile/
├── DesignSystem/
│   ├── Colors.swift
│   ├── Typography.swift
│   ├── Components/
│   │   ├── ScoreRing.swift          -- 环形分数组件
│   │   ├── RadarChart.swift         -- 雷达图
│   │   ├── RecordButton.swift       -- 录音按钮(脉冲动效)
│   │   └── PracticeCard.swift       -- 练习入口卡片
│   └── Animations/
└── Resources/
    ├── Assets.xcassets
    └── Lottie/
```

**音频录制核心逻辑：**

```swift
// AudioRecorder.swift 关键实现
class AudioRecorder: ObservableObject {
    private let audioEngine = AVAudioEngine()
    private var inputNode: AVAudioInputNode { audioEngine.inputNode }
    
    @Published var isRecording = false
    @Published var waveformData: [Float] = []
    
    func startRecording() {
        let format = inputNode.outputFormat(forBus: 0)
        
        // 安装音频采集节点
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, time in
            // 1. 提取波形数据用于可视化
            self.updateWaveform(buffer: buffer)
            
            // 2. 通过 WebSocket 实时发送音频流到讯飞 ASR
            self.streamToASR(buffer: buffer)
            
            // 3. 缓存到本地文件用于后续评测
            self.appendToFile(buffer: buffer)
        }
        
        audioEngine.prepare()
        try audioEngine.start()
        isRecording = true
    }
    
    func stopRecording() -> URL {
        inputNode.removeTap(onBus: 0)
        audioEngine.stop()
        isRecording = false
        return localFileURL
    }
}
```

**讯飞语音评测对接：**

```swift
// 发音评测请求（录音结束后调用）
struct PronunciationAssessment {
    static func evaluate(audioURL: URL, referenceText: String) async throws -> PronScore {
        // 上传音频到后端 → 后端调用讯飞评测 API
        let request = AssessmentRequest(
            audioData: try Data(contentsOf: audioURL),
            text: referenceText,
            category: "read_sentence",  // 朗读句子
            resultLevel: "complete"      // 返回音素级详情
        )
        return try await APIClient.shared.post("/api/v1/assessment", body: request)
    }
}
```

---

## 五、Web 教师后台设计方案

### 5.1 信息架构

```
教师后台
├── 📊 数据看板 (Dashboard)
│   ├── 班级整体数据概览
│   ├── 学生活跃度热力图
│   ├── 平均分趋势图
│   └── 弱项统计排行
│
├── 📚 资源管理 (Resources)
│   ├── 题库管理
│   │   ├── 按考试类型筛选
│   │   ├── 新建题目（富文本 + 音频上传）
│   │   └── 批量导入 (Excel/CSV)
│   ├── 教学资源
│   │   ├── 上传音频/文档/视频
│   │   └── 资源分类与标签
│   └── 范文库
│
├── 📝 作业管理 (Assignments)
│   ├── 创建作业（从题库选题 + 设置截止日期）
│   ├── 作业列表（进度追踪）
│   ├── 批改界面（听录音 + AI 辅助评分 + 手动调分）
│   └── 批量批改
│
├── 👥 班级管理 (Classes)
│   ├── 班级列表
│   ├── 学生管理（邀请/移除）
│   └── 单个学生详情（练习曲线、弱项分析）
│
└── ⚙️ 设置 (Settings)
    ├── 个人信息
    └── 通知设置
```

### 5.2 核心页面设计方案

#### 页面 1：数据看板 (Dashboard)

**设计理念：** 清晰的数据层次，教师一眼掌握全局

**配色方向：**
- 主色：深海蓝 (#0F172A) 侧边栏 + 白色 (#FFFFFF) 内容区
- 数据色：蓝 (#3B82F6)、绿 (#10B981)、橙 (#F59E0B)、红 (#EF4444)
- 字体：Geist Sans (标题) + 系统字体

```
┌──────┬──────────────────────────────────────────────────┐
│      │  📊 数据看板                    王老师 ▼  🔔      │
│ LOGO │──────────────────────────────────────────────────│
│      │                                                  │
│ 📊   │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐ │
│ 看板  │  │ 活跃学生 │ │ 本周练习 │ │ 平均分   │ │ 作业完 │ │
│      │  │   42    │ │  186次  │ │  82.5   │ │  成率  │ │
│ 📚   │  │  ↑12%   │ │  ↑8%   │ │  ↑3.2  │ │  89%  │ │
│ 资源  │  └─────────┘ └─────────┘ └─────────┘ └────────┘ │
│      │                                                  │
│ 📝   │  ┌── 评分趋势 ──────────────┐ ┌── 练习分布 ────┐  │
│ 作业  │  │                          │ │                │  │
│      │  │    📈 折线图              │ │  🍩 环形图     │  │
│ 👥   │  │    (各维度分数趋势)       │ │  (题型练习占比) │  │
│ 班级  │  │    过去30天              │ │                │  │
│      │  └──────────────────────────┘ └────────────────┘  │
│ ⚙️   │                                                  │
│ 设置  │  ┌── 学生排行 ─────────────────────────────────┐  │
│      │  │ 排名  学生     本周练习  平均分  趋势        │  │
│      │  │  1   张三      28次    92.3   ↑           │  │
│      │  │  2   李四      25次    88.7   ↑           │  │
│      │  │  3   王五      22次    85.1   →           │  │
│      │  │  ...                                      │  │
│      │  └──────────────────────────────────────────────┘  │
└──────┴──────────────────────────────────────────────────┘
```

---

#### 页面 2：题库管理 / 新建题目

```
┌──────┬──────────────────────────────────────────────────┐
│ 侧栏 │  📚 题库管理  >  新建题目                          │
│      │──────────────────────────────────────────────────│
│      │                                                  │
│      │  考试类型    [TOEFL ▼]                            │
│      │  题型       [Independent Task 1 ▼]               │
│      │  难度       [⭐⭐⭐ 中等]                         │
│      │                                                  │
│      │  题目内容                                         │
│      │  ┌──────────────────────────────────────────┐    │
│      │  │ Some people prefer to study alone, while │    │
│      │  │ others prefer to study in groups. Which  │    │
│      │  │ do you prefer and why?                   │    │
│      │  └──────────────────────────────────────────┘    │
│      │                                                  │
│      │  示范音频                                         │
│      │  ┌──────────────────────────────────────────┐    │
│      │  │  📎 拖拽上传音频文件 (mp3/wav/m4a)        │    │
│      │  │     或点击选择文件                         │    │
│      │  └──────────────────────────────────────────┘    │
│      │  ▶ sample_answer.mp3  (已上传, 1:23)             │
│      │                                                  │
│      │  参考答案文本                                      │
│      │  ┌──────────────────────────────────────────┐    │
│      │  │ 富文本编辑器...                            │    │
│      │  └──────────────────────────────────────────┘    │
│      │                                                  │
│      │  标签  [发音] [独立口语] [+ 添加]                  │
│      │                                                  │
│      │          [取消]    [保存为草稿]    [✅ 发布]       │
└──────┴──────────────────────────────────────────────────┘
```

---

#### 页面 3：作业批改界面

**设计理念：** 左右分栏，左侧听录音看数据，右侧写评语

```
┌──────┬──────────────────────┬───────────────────────────┐
│ 侧栏 │  学生录音 & AI 评分    │   教师批改区               │
│      │─────────────────────│───────────────────────────│
│      │                     │                           │
│      │  👤 张三             │   综合评分                 │
│      │  提交：04-02 14:30   │   AI 建议：88  →  [  88 ] │
│      │                     │   (可手动修改)              │
│      │  ▶ ━━━━━━━━━○ 1:23  │                           │
│      │  (音频播放器)         │   各维度调分               │
│      │                     │   发音 [92] 流利 [85]      │
│      │  ┌─ 转写文本 ──────┐ │   语法 [80] 内容 [88]      │
│      │  │ I personally   │ │                           │
│      │  │ prefer to      │ │   教师评语                 │
│      │  │ study {alone}  │ │   ┌─────────────────────┐ │
│      │  │ because...     │ │   │                     │ │
│      │  │ (问题词标红)     │ │   │  输入评语...         │ │
│      │  └────────────────┘ │   │                     │ │
│      │                     │   │                     │ │
│      │  ┌─ AI 评分详情 ──┐  │   └─────────────────────┘ │
│      │  │ 🟢 发音 92     │  │                           │
│      │  │   /θ/发音偏差   │  │   快捷评语模板             │
│      │  │ 🟢 流利 85     │  │   [发音很好] [注意语法]    │
│      │  │   停顿2次      │  │   [时间控制] [内容丰富]    │
│      │  │ 🟡 语法 80     │  │                           │
│      │  │   时态错误1处   │  │                           │
│      │  │ 🟢 内容 88     │  │                           │
│      │  └────────────────┘ │   [← 上一个] [保存] [下一个 →] │
└──────┴──────────────────────┴───────────────────────────┘
```

---

#### 页面 4：学生详情页

```
┌──────┬──────────────────────────────────────────────────┐
│ 侧栏 │  👤 张三 的学习档案                                │
│      │──────────────────────────────────────────────────│
│      │                                                  │
│      │  ┌── 基本信息 ───────┐ ┌── 目标 ──────────────┐   │
│      │  │ 班级：雅思冲刺A班  │ │ 目标分数：7.0        │   │
│      │  │ 加入：2026-01-15  │ │ 当前预估：6.5        │   │
│      │  │ 累计练习：86 次    │ │ 差距：↑ 0.5         │   │
│      │  └──────────────────┘ └───────────────────────┘   │
│      │                                                  │
│      │  ┌── 评分趋势（过去 60 天）────────────────────┐   │
│      │  │                                            │   │
│      │  │      📈 多折线图                            │   │
│      │  │      (发音/流利/语法/内容 四条线)            │   │
│      │  │                                            │   │
│      │  └────────────────────────────────────────────┘   │
│      │                                                  │
│      │  ┌── 能力雷达 ──────┐ ┌── 弱项分析 ───────────┐   │
│      │  │                  │ │ 🔴 /θ/ 发音 → 练习 5次│   │
│      │  │   🕸 雷达图       │ │ 🟡 虚拟语气 → 练习 3次│   │
│      │  │                  │ │ 🟡 Part 3 延展 → 2次  │   │
│      │  └──────────────────┘ └───────────────────────┘   │
│      │                                                  │
│      │  ┌── 最近练习记录 ──────────────────────────────┐  │
│      │  │ 日期      题型         分数  ▶              │  │
│      │  │ 04-02   IELTS Part2   88   [播放录音]      │  │
│      │  │ 04-01   IELTS Part1   92   [播放录音]      │  │
│      │  │ 03-31   跟读练习       85   [查看详情]      │  │
│      │  └──────────────────────────────────────────────┘  │
└──────┴──────────────────────────────────────────────────┘
```

### 5.3 Web 前端技术实现要点

**项目结构：**

```
teacher-dashboard/
├── app/
│   ├── layout.tsx                 -- 全局布局 (侧边栏 + 主内容)
│   ├── page.tsx                   -- Dashboard 首页
│   ├── resources/
│   │   ├── questions/page.tsx     -- 题库列表
│   │   ├── questions/new/page.tsx -- 新建题目
│   │   └── library/page.tsx       -- 教学资源库
│   ├── assignments/
│   │   ├── page.tsx               -- 作业列表
│   │   ├── new/page.tsx           -- 创建作业
│   │   └── [id]/grade/page.tsx    -- 批改界面
│   ├── classes/
│   │   ├── page.tsx               -- 班级列表
│   │   └── [id]/students/[sid]/page.tsx -- 学生详情
│   └── settings/page.tsx
├── components/
│   ├── layout/
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   └── Breadcrumb.tsx
│   ├── dashboard/
│   │   ├── StatsCard.tsx
│   │   ├── ScoreTrendChart.tsx
│   │   └── StudentRankTable.tsx
│   ├── grading/
│   │   ├── AudioPlayer.tsx        -- 带波形的音频播放器
│   │   ├── TranscriptViewer.tsx   -- 转写文本（问题词高亮）
│   │   └── ScoreAdjuster.tsx      -- 评分调整滑块
│   └── ui/                        -- Radix UI 基础组件封装
├── lib/
│   ├── api.ts                     -- API 客户端
│   ├── auth.ts                    -- JWT 认证
│   └── upload.ts                  -- OSS 直传签名
├── hooks/
│   ├── useAudioPlayer.ts
│   └── useDashboardData.ts
└── styles/
    └── globals.css
```

**文件上传方案（OSS 直传）：**

```typescript
// lib/upload.ts
export async function uploadToOSS(file: File, type: 'audio' | 'document') {
  // 1. 向后端请求临时上传凭证
  const { policy, signature, ossUrl, key } = await api.post('/upload/sign', {
    filename: file.name,
    contentType: file.type,
    folder: type,
  });
  
  // 2. 浏览器直传 OSS (不经过后端，节省带宽)
  const formData = new FormData();
  formData.append('key', key);
  formData.append('policy', policy);
  formData.append('signature', signature);
  formData.append('file', file);
  
  await fetch(ossUrl, { method: 'POST', body: formData });
  
  return `${ossUrl}/${key}`;
}
```

---

## 六、API 接口设计（核心）

### 6.1 RESTful API 概览

```
认证
POST   /api/v1/auth/login           -- 手机号/邮箱登录
POST   /api/v1/auth/register        -- 注册
POST   /api/v1/auth/refresh         -- 刷新 Token

练习（学生端）
POST   /api/v1/practice/start       -- 开始练习会话
POST   /api/v1/practice/audio       -- 上传录音
GET    /api/v1/practice/sessions     -- 练习记录列表
GET    /api/v1/practice/sessions/:id -- 练习详情 & 评分
GET    /api/v1/practice/stats        -- 练习统计数据

AI 对话（WebSocket）
WS     /api/v1/conversation/connect  -- 建立对话连接
       → client: {type: "audio_chunk", data: base64}   -- 流式发送音频
       ← server: {type: "transcript", text: "..."}     -- 实时转写
       ← server: {type: "examiner", text: "...", audio: "..."}  -- 考官回复
       ← server: {type: "score_update", scores: {...}}  -- 实时评分更新

题库
GET    /api/v1/questions             -- 题目列表 (支持筛选)
POST   /api/v1/questions             -- 新建题目 (教师)
PUT    /api/v1/questions/:id         -- 编辑题目
DELETE /api/v1/questions/:id         -- 删除题目
POST   /api/v1/questions/import      -- 批量导入

作业
POST   /api/v1/assignments           -- 创建作业 (教师)
GET    /api/v1/assignments           -- 作业列表
GET    /api/v1/assignments/:id       -- 作业详情
POST   /api/v1/assignments/:id/submit -- 提交作业 (学生)
PUT    /api/v1/assignments/:id/grade  -- 批改作业 (教师)

资源
POST   /api/v1/resources/upload-sign -- 获取 OSS 上传凭证
POST   /api/v1/resources             -- 创建资源记录
GET    /api/v1/resources             -- 资源列表

班级
POST   /api/v1/classes               -- 创建班级
GET    /api/v1/classes               -- 班级列表
POST   /api/v1/classes/:id/students  -- 添加学生
GET    /api/v1/classes/:id/analytics -- 班级数据分析

评测
POST   /api/v1/assessment/evaluate   -- 发音评测
POST   /api/v1/assessment/feedback   -- AI 反馈生成
POST   /api/v1/tts/synthesize        -- 语音合成（朗读示范）
```

---

## 七、开发计划（8个月）

### Phase 1：基础搭建（第 1-2 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| 后端项目初始化、数据库、认证系统 | 后端 | API 框架 + 用户系统 |
| iOS 项目架构搭建、Design System | iOS | 基础框架 + 组件库 |
| Web 后台项目搭建、布局框架 | 前端 | Next.js 项目 + 侧边栏布局 |
| 阿里云基础设施（ECS/OSS/RDS） | DevOps | 开发/测试环境 |
| 讯飞/通义千问 API 对接 & 联调 | 后端 | AI Orchestrator 服务 MVP |

### Phase 2：核心功能 - 练习模块（第 3-4 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| iOS 录音模块 + 波形可视化 | iOS | 录音核心能力 |
| iOS AI 对话界面 + WebSocket | iOS | 对话练习 MVP |
| iOS 跟读/朗读模式 | iOS | 跟读 & 朗读功能 |
| 后端练习服务 + 评分流水线 | 后端 | 完整评测流程 |
| AI 对话 Prompt Engineering | 后端 | 考官角色 Prompt 模板 |

### Phase 3：教师后台 - 资源 & 作业（第 4-5 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| Web 题库管理（CRUD + 上传） | 前端 | 题库管理页面 |
| Web 资源管理 + OSS 直传 | 前端 | 资源上传功能 |
| Web 作业创建 & 分发 | 前端 + 后端 | 作业系统 |
| iOS 作业模块 | iOS | 学生端作业功能 |

### Phase 4：批改 & 数据分析（第 5-6 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| Web 作业批改界面 | 前端 | 批改全流程 |
| Web 数据看板 + 图表 | 前端 | Dashboard |
| Web 学生详情页 | 前端 | 学生档案 |
| iOS 成长曲线 & 报告页 | iOS | 数据可视化 |
| 后端数据分析服务 | 后端 | 统计 API |

### Phase 5：模考 & 打磨（第 6-7 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| iOS 模考模式（完整计时流程） | iOS | 模考功能 |
| 全链路性能优化 | 全员 | 响应速度提升 |
| UI/UX 细节打磨 + 动效 | iOS + 前端 | 精品体验 |
| 离线缓存 + 弱网处理 | iOS | 稳定性 |

### Phase 6：测试 & 上线（第 7-8 月）

| 任务 | 负责 | 产出 |
|------|------|------|
| TestFlight 内测 + Bug 修复 | 全员 | 稳定版本 |
| App Store 审核提交 | iOS | 上架 |
| 安全审计 + 压力测试 | 后端 + DevOps | 安全报告 |
| 运营后台 + 数据埋点 | 全员 | 运营工具 |

### 团队配置建议

| 角色 | 人数 | 技术栈 |
|------|------|--------|
| iOS 开发 | 1-2 人 | Swift/SwiftUI |
| Web 前端 | 1 人 | Next.js/React/TypeScript |
| 后端开发 | 1-2 人 | Go/Node.js |
| AI/算法 | 1 人（可兼） | Prompt Engineering, 讯飞/千问对接 |
| UI 设计 | 1 人 | Figma |
| 测试 | 1 人 | 自动化测试 |

**最小 MVP 团队：3-4 人（iOS 1人 + 全栈 1人 + 后端/AI 1人 + 设计1人）**

---

## 八、成本估算（月度）

| 项目 | 预估月费 (RMB) |
|------|---------------|
| 阿里云 ECS (2核4G × 2) | ¥600 |
| 阿里云 RDS PostgreSQL | ¥400 |
| 阿里云 OSS (100GB) | ¥50 |
| Redis (2G) | ¥200 |
| 讯飞语音评测 (10万次/月) | ¥3,000 - ¥5,000 |
| 讯飞语音识别 | ¥1,000 - ¥2,000 |
| 讯飞语音合成 | ¥500 - ¥1,000 |
| 通义千问 API (qwen-max) | ¥2,000 - ¥5,000 |
| Apple Developer Account | ¥688/年 |
| **合计** | **¥8,000 - ¥15,000/月** |

> 注：AI API 费用与用户量强相关，以上按 500 活跃学生估算。讯飞有教育行业优惠套餐可进一步降低成本。

---

## 九、风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| 讯飞评测结果不稳定 | 评分一致性差 | 多次采样取均值 + 后端校准算法 |
| AI 对话质量不稳定 | 考官回复不专业 | 精细 Prompt + Few-shot 示例 + 输出格式约束 |
| 音频上传弱网问题 | 用户体验差 | 本地缓存录音 + 断点续传 + 后台自动重试 |
| App Store 审核拒绝 | 延迟上线 | 提前熟悉审核指南，首次提交预留 2 周缓冲 |
| AI API 成本超预期 | 运营亏损 | 设置用户每日练习次数限额 + 缓存高频 Prompt 结果 |
