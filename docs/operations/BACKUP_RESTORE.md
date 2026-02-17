# Backup & Restore Guide

Comprehensive backup and restore strategies for VaultStadio deployments.

---

## Overview

VaultStadio has two main data stores that require backup:

1. **PostgreSQL Database** - User data, metadata, settings
2. **File Storage** - Actual file content (local filesystem or S3)

Both must be backed up consistently to ensure full recovery.

---

## Database Backup

### PostgreSQL Dump

**Manual Backup:**

```bash
# Full database dump
pg_dump -h localhost -U vaultstadio -d vaultstadio -F c -f vaultstadio_backup.dump

# With timestamp
pg_dump -h localhost -U vaultstadio -d vaultstadio -F c -f "vaultstadio_$(date +%Y%m%d_%H%M%S).dump"
```

**Automated Backup Script:**

```bash
#!/bin/bash
# /scripts/backup-db.sh

BACKUP_DIR="/backups/database"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Create backup
pg_dump -h $DATABASE_HOST -U $DATABASE_USER -d vaultstadio -F c \
    -f "$BACKUP_DIR/vaultstadio_$TIMESTAMP.dump"

# Remove old backups
find $BACKUP_DIR -name "*.dump" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: vaultstadio_$TIMESTAMP.dump"
```

### Docker Compose Backup

```bash
# Backup from running container
docker-compose -f docker/docker-compose.yml exec postgres \
    pg_dump -U vaultstadio vaultstadio > backup.sql

# Compressed
docker-compose -f docker/docker-compose.yml exec postgres \
    pg_dump -U vaultstadio vaultstadio | gzip > backup.sql.gz
```

---

## File Storage Backup

### Local Storage

```bash
# rsync to backup location
rsync -avz --delete /data/storage/ /backups/storage/

# With timestamp (incremental)
rsync -avz /data/storage/ "/backups/storage_$(date +%Y%m%d)/"

# Compressed archive
tar -czvf "storage_$(date +%Y%m%d).tar.gz" /data/storage/
```

### S3/MinIO Storage

```bash
# Using AWS CLI
aws s3 sync s3://vaultstadio-bucket s3://vaultstadio-backup-bucket

# Using rclone
rclone sync minio:vaultstadio backup:vaultstadio-backup

# To local storage
aws s3 sync s3://vaultstadio-bucket /backups/s3/
```

---

## Complete Backup Strategy

### Daily Backup Script

```bash
#!/bin/bash
# /scripts/full-backup.sh

set -euo pipefail

BACKUP_ROOT="/backups"
DATE=$(date +%Y%m%d)
BACKUP_DIR="$BACKUP_ROOT/$DATE"

mkdir -p "$BACKUP_DIR"

echo "Starting VaultStadio backup: $DATE"

# 1. Database backup
echo "Backing up database..."
pg_dump -h $DATABASE_HOST -U $DATABASE_USER -d vaultstadio -F c \
    -f "$BACKUP_DIR/database.dump"

# 2. Storage backup
echo "Backing up storage..."
rsync -az /data/storage/ "$BACKUP_DIR/storage/"

# 3. Configuration backup
echo "Backing up configuration..."
cp /app/.env "$BACKUP_DIR/config.env" 2>/dev/null || true
cp -r /app/config "$BACKUP_DIR/config/" 2>/dev/null || true

# 4. Create manifest
cat > "$BACKUP_DIR/manifest.json" << EOF
{
    "date": "$DATE",
    "timestamp": "$(date -Iseconds)",
    "database": "database.dump",
    "storage": "storage/",
    "version": "$(cat /app/VERSION 2>/dev/null || echo 'unknown')"
}
EOF

# 5. Compress
echo "Compressing backup..."
cd "$BACKUP_ROOT"
tar -czf "vaultstadio_$DATE.tar.gz" "$DATE"
rm -rf "$DATE"

echo "Backup completed: vaultstadio_$DATE.tar.gz"
```

### Cron Schedule

```bash
# /etc/cron.d/vaultstadio-backup

# Daily backup at 2 AM
0 2 * * * root /scripts/full-backup.sh >> /var/log/vaultstadio-backup.log 2>&1

# Weekly full backup with extended retention
0 3 * * 0 root /scripts/full-backup.sh --weekly >> /var/log/vaultstadio-backup.log 2>&1
```

---

## Restore Procedures

### Database Restore

```bash
# Stop application first
docker-compose -f docker/docker-compose.yml stop backend

# Restore database
pg_restore -h localhost -U vaultstadio -d vaultstadio -c backup.dump

# Or from SQL dump
psql -h localhost -U vaultstadio -d vaultstadio < backup.sql

# Start application
docker-compose -f docker/docker-compose.yml start backend
```

### Storage Restore

```bash
# Stop application
docker-compose -f docker/docker-compose.yml stop backend

# Restore storage
rsync -avz /backups/storage/ /data/storage/

# Fix permissions
chown -R 1000:1000 /data/storage

# Start application
docker-compose -f docker/docker-compose.yml start backend
```

### Full Restore

```bash
#!/bin/bash
# /scripts/restore.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: restore.sh <backup_file.tar.gz>"
    exit 1
fi

# Stop services
docker-compose -f docker/docker-compose.yml stop

# Extract backup
RESTORE_DIR="/tmp/restore_$$"
mkdir -p "$RESTORE_DIR"
tar -xzf "$BACKUP_FILE" -C "$RESTORE_DIR"
BACKUP_DIR=$(ls "$RESTORE_DIR")

# Restore database
docker-compose -f docker/docker-compose.yml up -d postgres
sleep 5
docker-compose -f docker/docker-compose.yml exec -T postgres \
    pg_restore -U vaultstadio -d vaultstadio -c < "$RESTORE_DIR/$BACKUP_DIR/database.dump"

# Restore storage
rsync -avz "$RESTORE_DIR/$BACKUP_DIR/storage/" /data/storage/

# Cleanup
rm -rf "$RESTORE_DIR"

# Start services
docker-compose -f docker/docker-compose.yml up -d

echo "Restore completed"
```

---

## Backup Verification

### Test Restore

Regularly test backups by restoring to a test environment:

```bash
# Create test environment
docker-compose -f docker/docker-compose.test.yml up -d

# Restore to test
./scripts/restore.sh backup.tar.gz --target test

# Verify data integrity
./scripts/verify-backup.sh
```

### Integrity Checks

```bash
# Check database dump integrity
pg_restore -l backup.dump > /dev/null && echo "Database backup OK"

# Check storage integrity
find /data/storage -type f -exec md5sum {} \; > checksums.txt
# Compare with previous checksums
diff checksums.txt previous_checksums.txt
```

---

## Disaster Recovery

### Recovery Time Objective (RTO)

| Scenario | Target RTO |
|----------|------------|
| Database corruption | 15 minutes |
| Storage failure | 30 minutes |
| Full system failure | 1 hour |
| Datacenter failure | 4 hours |

### Recovery Point Objective (RPO)

| Backup Type | RPO |
|-------------|-----|
| Continuous (streaming) | < 1 minute |
| Hourly | 1 hour |
| Daily | 24 hours |

### Offsite Backup

```bash
# Sync to offsite location
rclone sync /backups offsite:vaultstadio-backups --progress

# Encrypted offsite backup
rclone sync /backups offsite:vaultstadio-backups --crypt-remote
```

---

## Best Practices

1. **Test restores regularly** - Monthly restore tests to staging
2. **Encrypt backups** - Use GPG or built-in encryption
3. **Multiple locations** - Keep backups in different physical locations
4. **Monitor backup jobs** - Alert on failures
5. **Document procedures** - Keep runbooks updated
6. **Version control configs** - Store non-secret configs in git

---

## See Also

- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide
- [DOCKER_BUILD.md](DOCKER_BUILD.md) - Docker configuration
- [HIGH_AVAILABILITY.md](HIGH_AVAILABILITY.md) - HA setup
