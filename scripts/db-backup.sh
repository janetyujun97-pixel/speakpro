#!/bin/bash
# SpeakPro 数据库备份脚本
# 用法: ./scripts/db-backup.sh [output_dir]

set -euo pipefail

# 加载环境变量
ENV_FILE="${ENV_FILE:-server/.env}"
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-speakpro}"
DB_USER="${DB_USER:-speakpro}"
OUTPUT_DIR="${1:-backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${OUTPUT_DIR}/speakpro_${TIMESTAMP}.sql.gz"

mkdir -p "$OUTPUT_DIR"

echo "正在备份数据库 ${DB_NAME}@${DB_HOST}:${DB_PORT}..."

PGPASSWORD="${DB_PASSWORD}" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --no-owner \
  --no-privileges \
  | gzip > "$BACKUP_FILE"

FILESIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "备份完成: $BACKUP_FILE ($FILESIZE)"

# 清理 30 天前的备份
find "$OUTPUT_DIR" -name "speakpro_*.sql.gz" -mtime +30 -delete 2>/dev/null || true
echo "已清理 30 天前的旧备份"
