# Performance Tuning Guide

Optimization strategies for VaultStadio deployments.

---

## JVM Configuration

### Memory Settings

```bash
# Production recommended settings
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# High-memory server (8GB+ RAM)
JAVA_OPTS="-Xms2g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Container with limited memory (2GB)
JAVA_OPTS="-Xms256m -Xmx1536m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

### GC Tuning

```bash
# G1GC (recommended for most workloads)
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:G1HeapRegionSize=16m"

# For low-latency requirements
JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"

# GC logging (debugging)
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=10m"
```

---

## Database Optimization

### PostgreSQL Configuration

```ini
# /etc/postgresql/postgresql.conf

# Memory
shared_buffers = 256MB              # 25% of RAM for dedicated server
effective_cache_size = 768MB        # 75% of RAM
work_mem = 16MB                     # Per-operation memory
maintenance_work_mem = 128MB        # For VACUUM, CREATE INDEX

# Connections
max_connections = 100               # Adjust based on pool size
 
# Write Performance
wal_buffers = 16MB
checkpoint_completion_target = 0.9
synchronous_commit = off            # For better write performance (risk: 1 tx loss)

# Query Planning
random_page_cost = 1.1              # For SSDs
effective_io_concurrency = 200      # For SSDs
```

### Connection Pool Settings

```kotlin
// Koin configuration
DatabaseConfig(
    maxPoolSize = 20,      // Match app server threads
    minIdle = 5,           // Keep connections warm
    idleTimeout = 600000,  // 10 minutes
    maxLifetime = 1800000, // 30 minutes
)
```

### Query Optimization

```sql
-- Add indexes for common queries
CREATE INDEX CONCURRENTLY idx_storage_items_parent ON storage_items(parent_id);
CREATE INDEX CONCURRENTLY idx_storage_items_owner ON storage_items(owner_id);
CREATE INDEX CONCURRENTLY idx_storage_items_name ON storage_items(name);
CREATE INDEX CONCURRENTLY idx_activities_user ON activities(user_id, created_at DESC);

-- Analyze tables regularly
ANALYZE storage_items;
ANALYZE activities;
ANALYZE users;
```

---

## Application Tuning

### Ktor Server Configuration

```kotlin
// Application.kt
install(ContentNegotiation) {
    json(Json {
        ignoreUnknownKeys = true
        isLenient = true
    })
}

// Connection limits
embeddedServer(Netty, port = 8080) {
    // Netty configuration
}.start(wait = true)
```

### Environment Variables

```bash
# Increase file upload limits
VAULTSTADIO_MAX_UPLOAD_SIZE=10737418240  # 10GB

# Optimize thumbnail generation
VAULTSTADIO_THUMBNAIL_QUALITY=80
VAULTSTADIO_THUMBNAIL_MAX_SIZE=256

# Caching
VAULTSTADIO_CACHE_TTL_SECONDS=300
VAULTSTADIO_METADATA_CACHE_SIZE=10000
```

---

## Storage Optimization

### Local Storage

```bash
# Use fast storage (SSD/NVMe)
# Mount with noatime for better performance
mount -o noatime,nodiratime /dev/ssd /data/storage

# Filesystem: XFS or ext4
mkfs.xfs -f /dev/ssd
```

### S3/MinIO Configuration

```bash
# Increase connection pool
AWS_SDK_HTTP_CLIENT_MAX_CONNECTIONS=100

# Enable transfer acceleration (AWS S3)
AWS_S3_TRANSFER_ACCELERATION=true

# Multipart upload threshold
VAULTSTADIO_MULTIPART_THRESHOLD=104857600  # 100MB
VAULTSTADIO_MULTIPART_CHUNK_SIZE=10485760  # 10MB
```

---

## Caching Strategies

### Application-Level Caching

```kotlin
// In-memory cache for frequently accessed data
val thumbnailCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(Duration.ofMinutes(30))
    .build<String, ByteArray>()

val metadataCache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(Duration.ofMinutes(5))
    .build<String, StorageItemMetadata>()
```

### Redis Cache (Optional)

```yaml
# docker-compose.yml
services:
  redis:
    image: redis:7-alpine
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

```bash
# Environment configuration
VAULTSTADIO_CACHE_TYPE=redis
VAULTSTADIO_REDIS_URL=redis://redis:6379
```

---

## Load Testing

### Apache Bench

```bash
# Simple load test
ab -n 1000 -c 50 http://localhost:8080/api/health

# With authentication
ab -n 1000 -c 50 -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/storage/folder
```

### k6 Load Test Script

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
    ],
};

export default function() {
    let res = http.get('http://localhost:8080/api/health');
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}
```

```bash
k6 run load-test.js
```

---

## Monitoring Performance

### Key Metrics

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Response Time (p95) | < 200ms | > 500ms | > 1s |
| Response Time (p99) | < 500ms | > 1s | > 2s |
| Error Rate | < 0.1% | > 1% | > 5% |
| CPU Usage | < 70% | > 80% | > 90% |
| Memory Usage | < 80% | > 85% | > 95% |
| DB Connections | < 80% | > 90% | > 95% |

### Prometheus Metrics

```bash
# Enable metrics endpoint
VAULTSTADIO_METRICS_ENABLED=true
VAULTSTADIO_METRICS_PATH=/metrics
```

---

## Common Performance Issues

### Slow File Uploads

1. Check network bandwidth
2. Increase multipart chunk size
3. Use chunked upload API for large files
4. Consider S3 transfer acceleration

### Slow API Responses

1. Add database indexes
2. Enable query caching
3. Check connection pool exhaustion
4. Profile with JVM profiler

### High Memory Usage

1. Reduce thumbnail cache size
2. Lower connection pool limits
3. Tune JVM heap size
4. Check for memory leaks

### Database Connection Exhaustion

1. Increase pool size (with DB limits)
2. Reduce connection timeout
3. Fix connection leaks
4. Add connection monitoring

---

## Benchmarks

### Expected Performance (Reference Hardware)

| Operation | Target | Hardware |
|-----------|--------|----------|
| File list (100 items) | < 50ms | 4 CPU, 8GB RAM |
| File upload (100MB) | < 5s | Gigabit network |
| Thumbnail generation | < 200ms | 4 CPU |
| Search (10k files) | < 100ms | SSD storage |

---

## See Also

- [MONITORING.md](MONITORING.md) - Metrics and observability
- [CONFIGURATION.md](../getting-started/CONFIGURATION.md) - Environment variables
- [HIGH_AVAILABILITY.md](HIGH_AVAILABILITY.md) - Scaling strategies
