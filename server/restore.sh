#!/bin/bash
# 从备份恢复
# 用法：./restore.sh backups/llmgateway_YYYYMMDD_HHMMSS.sql.gz

set -e

if [ -z "$1" ]; then
    echo "用法: $0 <备份文件路径>"
    echo "示例: $0 backups/llmgateway_20240101_030000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "错误: 备份文件不存在: $BACKUP_FILE"
    exit 1
fi

if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

DB_USER="${POSTGRES_USER:-llmgateway}"
DB_NAME="${POSTGRES_DB:-llmgateway}"
CONTAINER="llm-gateway-db"

echo "⚠️  即将恢复数据库: $DB_NAME"
echo "   备份文件: $BACKUP_FILE"
echo "   当前数据将被覆盖！"
read -p "确认恢复？输入 YES 继续: " confirm

if [ "$confirm" != "YES" ]; then
    echo "已取消"
    exit 0
fi

echo "[$(date)] 停止应用服务..."
docker stop llm-gateway-app

echo "[$(date)] 恢复数据库..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"

echo "[$(date)] 重启应用服务..."
docker start llm-gateway-app

echo "[$(date)] 恢复完成"
