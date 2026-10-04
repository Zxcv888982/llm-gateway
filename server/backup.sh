#!/bin/bash
# PostgreSQL 自动备份脚本
# 用法：./backup.sh [保留天数]
# 建议加入 crontab：0 3 * * * /path/to/backup.sh 30

set -e

BACKUP_DIR="$(cd "$(dirname "$0")" && pwd)/backups"
RETENTION_DAYS="${1:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="llmgateway_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] 开始备份..."

# 从 docker-compose 环境变量读取数据库配置
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

DB_USER="${POSTGRES_USER:-llmgateway}"
DB_NAME="${POSTGRES_DB:-llmgateway}"
CONTAINER="llm-gateway-db"

# 执行备份
docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/$FILENAME"

# 验证备份
if [ -s "$BACKUP_DIR/$FILENAME" ]; then
    SIZE=$(du -h "$BACKUP_DIR/$FILENAME" | cut -f1)
    echo "[$(date)] 备份成功: $FILENAME ($SIZE)"
else
    echo "[$(date)] 备份失败: 文件为空"
    rm -f "$BACKUP_DIR/$FILENAME"
    exit 1
fi

# 清理过期备份
echo "[$(date)] 清理 ${RETENTION_DAYS} 天前的备份..."
find "$BACKUP_DIR" -name "llmgateway_*.sql.gz" -type f -mtime +"$RETENTION_DAYS" -delete -print

echo "[$(date)] 备份完成"
echo "---"
ls -lh "$BACKUP_DIR" | tail -5
