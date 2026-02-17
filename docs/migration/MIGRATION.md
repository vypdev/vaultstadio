# VaultStadio Migration Guide

This guide covers upgrading VaultStadio between versions and migrating data.

## Version Compatibility

| From Version | To Version | Migration Type | Downtime |
|--------------|------------|----------------|----------|
| 1.x | 2.x | Major (breaking) | Required |
| 2.0 | 2.1 | Minor (compatible) | Optional |
| 2.1 | 2.2 | Patch | None |

---

## Pre-Migration Checklist

Before any migration:

- [ ] Read release notes for target version
- [ ] Backup database completely
- [ ] Backup storage files
- [ ] Test migration in staging environment
- [ ] Schedule maintenance window
- [ ] Notify users of downtime
- [ ] Document current configuration

---

## Backup Procedures

### Database Backup

```bash
# PostgreSQL backup
pg_dump -Fc -f vaultstadio_backup_$(date +%Y%m%d).dump vaultstadio

# Verify backup
pg_restore --list vaultstadio_backup_*.dump
```

### Storage Backup

```bash
# Local storage
tar -czf storage_backup_$(date +%Y%m%d).tar.gz /data/vaultstadio/

# S3/MinIO
aws s3 sync s3://vaultstadio-bucket ./backup/
```

### Configuration Backup

```bash
# Docker environment
cp .env .env.backup_$(date +%Y%m%d)
cp docker-compose.yml docker-compose.yml.backup

# Kubernetes
kubectl get configmap vaultstadio-config -o yaml > config_backup.yaml
kubectl get secret vaultstadio-secrets -o yaml > secrets_backup.yaml
```

---

## Database Migrations

VaultStadio uses automatic database migrations on startup.

### Automatic Migration

Migrations run automatically when the application starts:

```yaml
# docker-compose.yml
environment:
  DATABASE_AUTO_MIGRATE: "true"
```

### Manual Migration

For production, you may prefer manual migrations:

```bash
# Run migrations only (don't start server)
docker run --rm \
  -e DATABASE_URL="..." \
  vaultstadio/backend:2.0.0 \
  migrate

# Check migration status
docker run --rm \
  -e DATABASE_URL="..." \
  vaultstadio/backend:2.0.0 \
  migrate status
```

### Migration Scripts

Migration scripts are located in:
```
kotlin-backend/infrastructure/src/main/resources/db/migration/
```

Naming convention:
```
V{version}__{description}.sql
V1__initial_schema.sql
V2__add_versioning.sql
V3__add_federation.sql
```

---

## Upgrading Docker Deployment

### Standard Upgrade

```bash
# Pull new images
docker-compose pull

# Stop services
docker-compose down

# Start with new version
docker-compose up -d

# Check logs
docker-compose logs -f backend
```

### Rolling Update (Zero Downtime)

For deployments with multiple instances:

```bash
# Scale up new version
docker-compose up -d --scale backend=2

# Wait for health checks
sleep 30

# Remove old instance
docker stop vaultstadio_backend_1

# Verify
curl http://localhost:8080/health
```

---

## Upgrading Kubernetes/Helm

### Helm Upgrade

```bash
# Update Helm repository
helm repo update

# Review changes
helm diff upgrade vaultstadio vaultstadio/vaultstadio --version 2.0.0

# Upgrade
helm upgrade vaultstadio vaultstadio/vaultstadio \
  --version 2.0.0 \
  --values values.yaml \
  --wait

# Rollback if needed
helm rollback vaultstadio 1
```

### Manual Kubernetes Upgrade

```bash
# Update image
kubectl set image deployment/vaultstadio-backend \
  backend=vaultstadio/backend:2.0.0

# Monitor rollout
kubectl rollout status deployment/vaultstadio-backend

# Rollback if needed
kubectl rollout undo deployment/vaultstadio-backend
```

---

## Version-Specific Migrations

### 1.x to 2.0

**Breaking Changes:**
- New database schema for versioning
- Changed API authentication flow
- New configuration format

**Migration Steps:**

1. **Backup everything:**
   ```bash
   ./scripts/backup.sh full
   ```

2. **Export data from 1.x:**
   ```bash
   docker exec vaultstadio-backend-1x ./export-data.sh > data.json
   ```

3. **Update configuration:**
   ```bash
   # Old format
   DB_HOST=localhost
   DB_PORT=5432
   
   # New format
   DATABASE_URL=postgresql://user:pass@localhost:5432/vaultstadio
   ```

4. **Run database migrations:**
   ```bash
   docker run --rm vaultstadio/backend:2.0.0 migrate
   ```

5. **Import data:**
   ```bash
   docker exec vaultstadio-backend ./import-data.sh < data.json
   ```

6. **Verify:**
   ```bash
   curl http://localhost:8080/health
   ```

### 2.0 to 2.1

**Changes:**
- Added AI integration
- New sync protocol
- Federation improvements

**Migration Steps:**

1. **Update configuration (optional new features):**
   ```yaml
   # New optional settings
   AI_ENABLED: true
   AI_PROVIDER: ollama
   AI_BASE_URL: http://localhost:11434
   
   SYNC_ENABLED: true
   FEDERATION_ENABLED: true
   ```

2. **Run upgrade:**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

3. **Initialize new features (if enabled):**
   ```bash
   # AI models will be pulled automatically
   # Federation key pair generated on first start
   ```

---

## Storage Backend Migration

### Local to S3/MinIO

```bash
# 1. Install rclone
curl https://rclone.org/install.sh | sudo bash

# 2. Configure rclone
rclone config
# Add source (local) and destination (s3)

# 3. Copy data
rclone copy local:/data/vaultstadio s3:vaultstadio-bucket --progress

# 4. Verify
rclone check local:/data/vaultstadio s3:vaultstadio-bucket

# 5. Update configuration
STORAGE_BACKEND=s3
S3_ENDPOINT=https://minio.example.com
S3_BUCKET=vaultstadio-bucket
S3_ACCESS_KEY=...
S3_SECRET_KEY=...
```

### S3 to Different S3

```bash
# Use rclone for S3-to-S3 migration
rclone sync old-s3:bucket new-s3:bucket --progress
```

---

## Database Migration (PostgreSQL Versions)

### Upgrade PostgreSQL

```bash
# 1. Backup with old version
docker exec postgres-old pg_dump -Fc vaultstadio > backup.dump

# 2. Stop old container
docker stop postgres-old

# 3. Start new version
docker run -d --name postgres-new \
  -e POSTGRES_PASSWORD=... \
  -v pgdata-new:/var/lib/postgresql/data \
  postgres:16

# 4. Restore
docker exec -i postgres-new pg_restore -d vaultstadio < backup.dump

# 5. Update connection string and restart VaultStadio
```

---

## Rollback Procedures

### Docker Rollback

```bash
# Stop current version
docker-compose down

# Restore database
pg_restore -c -d vaultstadio vaultstadio_backup_*.dump

# Start previous version
docker-compose -f docker-compose.yml.backup up -d
```

### Kubernetes Rollback

```bash
# Rollback deployment
kubectl rollout undo deployment/vaultstadio-backend

# Or rollback to specific revision
kubectl rollout undo deployment/vaultstadio-backend --to-revision=2
```

### Helm Rollback

```bash
# View history
helm history vaultstadio

# Rollback
helm rollback vaultstadio 1
```

---

## Data Export/Import

### Full Data Export

```bash
# Export all user data
curl -X GET /api/v1/admin/export \
  -H "Authorization: Bearer <admin-token>" \
  -o full_export.json

# Export specific user
curl -X GET /api/v1/admin/users/{userId}/export \
  -H "Authorization: Bearer <admin-token>" \
  -o user_export.json
```

### Data Import

```bash
# Import data
curl -X POST /api/v1/admin/import \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d @full_export.json
```

---

## Health Checks During Migration

### Verify Application Health

```bash
# Basic health
curl http://localhost:8080/health

# Detailed health
curl http://localhost:8080/health/detailed

# Readiness
curl http://localhost:8080/ready
```

### Database Connectivity

```bash
# Check database connection
docker exec vaultstadio-backend ./check-db.sh

# Or via psql
psql $DATABASE_URL -c "SELECT 1"
```

### Storage Connectivity

```bash
# Check storage access
curl http://localhost:8080/api/v1/admin/storage/check \
  -H "Authorization: Bearer <admin-token>"
```

---

## Troubleshooting

### Migration Failed

```bash
# Check migration status
docker exec vaultstadio-backend ./check-migrations.sh

# View failed migration
docker logs vaultstadio-backend | grep -i migration

# Manual fix and retry
psql $DATABASE_URL -f fix_migration.sql
docker restart vaultstadio-backend
```

### Data Corruption

```bash
# Check data integrity
docker exec vaultstadio-backend ./check-integrity.sh

# Rebuild indexes
psql $DATABASE_URL -c "REINDEX DATABASE vaultstadio"

# Vacuum and analyze
psql $DATABASE_URL -c "VACUUM ANALYZE"
```

### Performance Issues After Migration

```bash
# Update statistics
psql $DATABASE_URL -c "ANALYZE"

# Check slow queries
docker exec vaultstadio-backend cat /var/log/slow_queries.log

# Review connection pool
curl http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer <admin-token>" \
  | jq '.database'
```

---

## Migration Automation

### CI/CD Pipeline Example

```yaml
# .github/workflows/migration.yml
name: Database Migration

on:
  push:
    branches: [main]
    paths:
      - 'kotlin-backend/infrastructure/src/main/resources/db/migration/**'

jobs:
  migrate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Backup database
        run: |
          pg_dump ${{ secrets.DATABASE_URL }} > backup.sql
          
      - name: Run migrations
        run: |
          docker run --rm \
            -e DATABASE_URL=${{ secrets.DATABASE_URL }} \
            vaultstadio/backend:${{ github.sha }} \
            migrate
            
      - name: Verify
        run: |
          curl -f ${{ secrets.APP_URL }}/health
```

---

## Support

For migration assistance:

- Check [Troubleshooting Guide](../getting-started/TROUBLESHOOTING.md)
- Review [GitHub Issues](https://github.com/vaultstadio/vaultstadio/issues)
- Join community Discord for help
