.PHONY: dev-infra dev-nest dev-go dev-web migrate help

help: ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# === 基础设施 ===

dev-infra: ## 启动 PostgreSQL + Redis (Docker)
	cd server && docker compose up -d postgres redis

dev-infra-all: ## 启动全部基础设施 (含 Nginx)
	cd server && docker compose up -d

dev-down: ## 停止全部容器
	cd server && docker compose down

# === 数据库 ===

migrate: ## 执行数据库迁移
	cd server && docker compose exec postgres psql -U speakpro -d speakpro -f /migrations/001_initial_schema.sql

# === 后端服务 ===

dev-nest: ## 启动 NestJS 开发服务器
	cd server/nest-api && npm run start:dev

dev-go: ## 启动 Go 开发服务器
	cd server/go-services && go run cmd/server/main.go

# === 前端 ===

dev-web: ## 启动 Web 教师后台开发服务器
	cd web && npm run dev

# === 安装依赖 ===

install: ## 安装所有依赖
	cd server/nest-api && npm install
	cd web && npm install
	cd server/go-services && go mod tidy

# === 构建 ===

build-nest: ## 构建 NestJS
	cd server/nest-api && npm run build

build-go: ## 构建 Go 服务
	cd server/go-services && go build -o bin/server cmd/server/main.go

build-web: ## 构建 Web 前端
	cd web && npm run build
