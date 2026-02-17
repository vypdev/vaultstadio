# Configuration Guide

## Environment Variables

### Application Settings

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VAULTSTADIO_APP_NAME` | Application name | `VaultStadio` | No |
| `VAULTSTADIO_ENVIRONMENT` | Environment (development/production) | `development` | No |
| `VAULTSTADIO_HOST` | Server host | `0.0.0.0` | No |
| `VAULTSTADIO_PORT` | Server port | `8080` | No |

### Database Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/vaultstadio` | Yes |
| `DATABASE_USER` | Database username | `vaultstadio` | Yes |
| `DATABASE_PASSWORD` | Database password | `vaultstadio` | Yes |
| `DATABASE_MAX_POOL_SIZE` | Connection pool size | `10` | No |

### Storage Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VAULTSTADIO_STORAGE_PATH` | Path to file storage | `/data/storage` | Yes |
| `VAULTSTADIO_DATA_PATH` | Path to plugin data | `/data/plugins` | No |
| `VAULTSTADIO_TEMP_PATH` | Path to temp files | `/data/temp` | No |
| `VAULTSTADIO_MAX_UPLOAD_SIZE` | Max upload size (bytes) | `10737418240` (10GB) | No |

### Security Settings

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VAULTSTADIO_JWT_SECRET` | JWT signing secret | (random) | **Yes** (prod) |
| `VAULTSTADIO_JWT_EXPIRATION` | JWT expiration (hours) | `24` | No |
| `VAULTSTADIO_CORS_ORIGINS` | Allowed CORS origins | `*` | No |

### Logging Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VAULTSTADIO_LOG_LEVEL` | Log level | `INFO` | No |
| `JAVA_OPTS` | JVM options | `-Xms256m -Xmx1024m` | No |

## Configuration Methods

### 1. Environment Variables (Recommended)

Set environment variables in your deployment:

```bash
export DATABASE_URL=jdbc:postgresql://db.example.com:5432/vaultstadio
export DATABASE_USER=myuser
export DATABASE_PASSWORD=securepassword
export VAULTSTADIO_STORAGE_PATH=/mnt/storage
export VAULTSTADIO_JWT_SECRET=your-super-secret-key-here
```

### 2. .env File (Development)

Create a `.env` file based on `docker/.env.example`:

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/vaultstadio
DATABASE_USER=vaultstadio
DATABASE_PASSWORD=vaultstadio

# Storage
VAULTSTADIO_STORAGE_PATH=/data/storage
VAULTSTADIO_DATA_PATH=/data/plugins

# Security
VAULTSTADIO_JWT_SECRET=development-secret-key

# Logging
VAULTSTADIO_LOG_LEVEL=DEBUG
```

### 3. Docker Compose

Set variables in `docker-compose.yml`:

```yaml
services:
  backend:
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/vaultstadio
      DATABASE_USER: ${POSTGRES_USER:-vaultstadio}
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-vaultstadio}
      VAULTSTADIO_STORAGE_PATH: /data/storage
      VAULTSTADIO_JWT_SECRET: ${JWT_SECRET:-change-in-production}
```

### 4. Helm Values (Kubernetes/TrueNAS)

Edit `helm/vaultstadio/values.yaml`:

```yaml
backend:
  env:
    DATABASE_URL: jdbc:postgresql://postgres:5432/vaultstadio
    VAULTSTADIO_STORAGE_PATH: /data/storage
    VAULTSTADIO_JWT_SECRET: "your-production-secret"
  
  persistence:
    storage:
      enabled: true
      size: 100Gi
      hostPath: /mnt/pool/vaultstadio
```

## Application Configuration (Kotlin)

The backend uses a typed configuration system:

```kotlin
// AppConfig.kt
data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val security: SecurityConfig
)

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val environment: String = "development"
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10
)
```

Configuration is loaded from environment variables automatically.

## Storage Backends

### Local Storage (Default)

```env
VAULTSTADIO_STORAGE_TYPE=local
VAULTSTADIO_STORAGE_PATH=/data/storage
```

### S3 Compatible (MinIO)

```env
VAULTSTADIO_STORAGE_TYPE=s3
VAULTSTADIO_S3_ENDPOINT=http://minio:9000
VAULTSTADIO_S3_BUCKET=vaultstadio
VAULTSTADIO_S3_ACCESS_KEY=minioadmin
VAULTSTADIO_S3_SECRET_KEY=minioadmin
```

## Plugin Configuration

Plugins are automatically loaded from the plugins directory:

```env
VAULTSTADIO_DATA_PATH=/data/plugins
```

Individual plugins can be configured via environment:

```env
# AI Plugin
AI_PLUGIN_API_KEY=your-openai-key
AI_PLUGIN_MODEL=gpt-4-vision-preview

# Search Plugin
SEARCH_PLUGIN_INDEX_PATH=/data/search-index
```

## Phase 6: Advanced Features Configuration

### File Versioning

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_VERSIONING_ENABLED` | Enable file versioning | `true` |
| `VAULTSTADIO_VERSIONING_MAX_VERSIONS` | Max versions per file | `10` |
| `VAULTSTADIO_VERSIONING_MAX_AGE_DAYS` | Delete versions older than | `90` |
| `VAULTSTADIO_VERSIONING_MIN_KEEP` | Minimum versions to keep | `1` |

### Sync Protocol

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_SYNC_ENABLED` | Enable sync protocol | `true` |
| `VAULTSTADIO_SYNC_MAX_DEVICES_PER_USER` | Max devices per user | `10` |
| `VAULTSTADIO_SYNC_DELTA_BLOCK_SIZE` | Block size for delta sync | `4096` |

### Real-time Collaboration

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_COLLAB_ENABLED` | Enable collaboration | `true` |
| `VAULTSTADIO_COLLAB_SESSION_TIMEOUT_HOURS` | Session timeout | `24` |
| `VAULTSTADIO_COLLAB_MAX_PARTICIPANTS` | Max users per session | `50` |

### Federation

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_FEDERATION_ENABLED` | Enable federation | `false` |
| `VAULTSTADIO_FEDERATION_DOMAIN` | Instance domain | (required if enabled) |
| `VAULTSTADIO_FEDERATION_NAME` | Instance display name | `VaultStadio Instance` |
| `FEDERATION_PUBLIC_KEY` | Ed25519 public key (Base64) | (auto-generated if empty) |
| `FEDERATION_PRIVATE_KEY` | Ed25519 private key (Base64) | (auto-generated if empty) |

#### Generating Federation Keys

VaultStadio uses Ed25519 cryptographic keys for secure federation communication.

**Option 1: Auto-generation (Development)**

If keys are not provided, VaultStadio generates a new key pair on startup. These keys are ephemeral and change on restart.

**Option 2: Manual Generation (Production)**

Use the provided script to generate persistent keys:

```bash
# Generate keys
./scripts/generate-federation-keys.sh

# Or save to file
./scripts/generate-federation-keys.sh --output /path/to/keys
```

This outputs Base64-encoded keys for your `.env` file:

```env
FEDERATION_PRIVATE_KEY=MC4CAQAwBQYDK2VwBCIE...
FEDERATION_PUBLIC_KEY=MCowBQYDK2VwAyEA...
```

**Security Requirements:**
- Never commit private keys to version control
- Store private keys in a secrets manager
- Each VaultStadio instance needs its own unique key pair
- Share only the public key with federated instances

### WebDAV

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_WEBDAV_ENABLED` | Enable WebDAV endpoint | `true` |
| `VAULTSTADIO_WEBDAV_REQUIRE_AUTH` | Require authentication | `true` |

### S3-Compatible API

For any backend instance, the S3 API (`/s3/*`) is **only available when that instance’s storage backend is S3 or MinIO**. Set `STORAGE_TYPE=S3` or `STORAGE_TYPE=MINIO` (and the S3/MinIO env vars) for that instance; see [Storage Configuration](../operations/STORAGE_CONFIGURATION.md). With default `STORAGE_TYPE=LOCAL`, the S3 API is not exposed.

| Variable | Description | Default |
|----------|-------------|---------|
| `STORAGE_TYPE` | Set to `S3` or `MINIO` to enable S3 API and S3 storage | `LOCAL` |
| S3_* (when S3) | `S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, etc. | See [Storage Configuration](../operations/STORAGE_CONFIGURATION.md) |

### Example Phase 6 Configuration

```env
# Phase 6: Advanced Features
VAULTSTADIO_VERSIONING_ENABLED=true
VAULTSTADIO_VERSIONING_MAX_VERSIONS=10
VAULTSTADIO_VERSIONING_MAX_AGE_DAYS=90

VAULTSTADIO_SYNC_ENABLED=true
VAULTSTADIO_SYNC_MAX_DEVICES_PER_USER=10

VAULTSTADIO_COLLAB_ENABLED=true
VAULTSTADIO_COLLAB_SESSION_TIMEOUT_HOURS=24

VAULTSTADIO_FEDERATION_ENABLED=true
VAULTSTADIO_FEDERATION_DOMAIN=storage.example.com
# Generate with: ./scripts/generate-federation-keys.sh
FEDERATION_PRIVATE_KEY=your-base64-private-key
FEDERATION_PUBLIC_KEY=your-base64-public-key

VAULTSTADIO_WEBDAV_ENABLED=true
# S3 API: set STORAGE_TYPE=S3 or MINIO and S3_* variables (see Storage Configuration)
```

See [Phase 6 Advanced Features](../api/PHASE6_ADVANCED_FEATURES.md) and [Storage Configuration](../operations/STORAGE_CONFIGURATION.md) for S3 setup.

---

## Production Recommendations

1. **Use strong secrets**: Generate secure random values for JWT_SECRET
2. **Limit CORS**: Set specific origins instead of `*`
3. **Configure logging**: Use `INFO` level in production
4. **Set resource limits**: Configure appropriate JVM memory settings
5. **Use external database**: Don't use SQLite in production
6. **Enable HTTPS**: Use a reverse proxy (Nginx, Traefik) for TLS

```env
# Production example
VAULTSTADIO_ENVIRONMENT=production
VAULTSTADIO_LOG_LEVEL=INFO
VAULTSTADIO_JWT_SECRET=<32+ character random string>
VAULTSTADIO_CORS_ORIGINS=https://vaultstadio.example.com
JAVA_OPTS=-Xms512m -Xmx2048m
```
