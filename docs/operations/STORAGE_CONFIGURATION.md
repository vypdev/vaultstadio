# Storage Configuration Guide

## Overview

VaultStadio supports flexible storage configurations:

- **Local Storage**: Files stored on local filesystem (default; no extra setup)
- **S3 Compatible**: MinIO, AWS S3, or other S3-compatible storage (optional)
- **Host Path**: Direct TrueNAS pool access

**S3 is optional.** For any backend instance, the default `STORAGE_TYPE=LOCAL` means the server uses the local filesystem only and the S3-compatible HTTP API (`/s3/*`) is **not** registered. The `/s3` routes are **only registered** when that instance has `STORAGE_TYPE=S3` or `STORAGE_TYPE=MINIO`. No S3 configuration is required to run VaultStadio with local storage.

## Storage Architecture

```
VaultStadio Storage
├── /data/storage/          # Main file storage
│   ├── {user-id}/          # User-specific files
│   │   ├── {file-uuid}     # Actual file content
│   │   └── ...
│   └── shared/             # Shared files
│
├── /data/plugins/          # Plugin data
│   ├── thumbnails/         # Generated thumbnails
│   ├── search-index/       # Full-text search index
│   └── metadata-cache/     # Extracted metadata
│
└── PostgreSQL              # Database
    ├── users               # User accounts
    ├── storage_items       # File/folder metadata
    ├── shares              # Share links
    └── ...
```

## Configuration Options

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULTSTADIO_STORAGE_PATH` | Main storage directory | `/data/storage` |
| `VAULTSTADIO_DATA_PATH` | Plugin data directory | `/data/plugins` |
| `VAULTSTADIO_TEMP_PATH` | Temporary files | `/data/temp` |
| `VAULTSTADIO_MAX_UPLOAD_SIZE` | Max file size (bytes) | `10737418240` (10GB) |

### Storage Type Configuration

#### Local Storage (Default)

No S3 configuration is required. The S3 API is not exposed.

```env
STORAGE_TYPE=LOCAL
STORAGE_LOCAL_PATH=/data/storage
```

(Docker/Helm may use a `VAULTSTADIO_` prefix for these variables; the backend reads `STORAGE_TYPE`, `STORAGE_LOCAL_PATH`, etc. as documented in `kotlin-backend/api/.env.example`.)

#### S3 Compatible (Optional)

Use S3 or MinIO as the storage backend and expose the S3-compatible API at `/s3`. **Required only if you set `STORAGE_TYPE=S3` or `STORAGE_TYPE=MINIO`.**

**Requirements to use S3:**

1. Set `STORAGE_TYPE=S3` or `STORAGE_TYPE=MINIO`.
2. Configure the following environment variables (no defaults for production):
   - `S3_ENDPOINT` – S3/MinIO endpoint URL (e.g. `http://minio:9000` or `https://s3.amazonaws.com`).
   - `S3_REGION` – AWS region or any value for MinIO (e.g. `us-east-1`).
   - `S3_BUCKET` – Bucket name.
   - `S3_ACCESS_KEY` – Access key.
   - `S3_SECRET_KEY` – Secret key.
3. Optional: `S3_USE_PATH_STYLE=true` for MinIO-style path URLs.

When `STORAGE_TYPE` is `S3` or `MINIO`:

- The backend uses the configured S3 bucket for storing file bytes.
- The HTTP routes under `/s3` are registered so that tools (rclone, AWS CLI, s3cmd) can use the server as an S3-compatible endpoint.
- Authentication: clients can use **JWT Bearer** (session token) or **AWS Signature V4** (when the `s3-signature` auth provider is implemented). With JWT, the same login token used for the REST API works for `/s3`.

**Example (MinIO):**

```env
STORAGE_TYPE=S3
S3_ENDPOINT=http://minio:9000
S3_REGION=us-east-1
S3_BUCKET=vaultstadio
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_USE_PATH_STYLE=true
```

**Example (AWS S3):**

```env
STORAGE_TYPE=S3
S3_ENDPOINT=https://s3.amazonaws.com
S3_REGION=eu-west-1
S3_BUCKET=my-vaultstadio-bucket
S3_ACCESS_KEY=AKIA...
S3_SECRET_KEY=...
S3_USE_PATH_STYLE=false
```

## TrueNAS Scale Configuration

### Option 1: Host Path (Recommended)

Use a TrueNAS dataset directly:

```yaml
# Helm values.yaml
backend:
  persistence:
    hostPath:
      enabled: true
      path: /mnt/pool/vaultstadio
```

**Setup Steps:**

1. Create Dataset in TrueNAS:
   ```
   Storage → Pools → your-pool → Add Dataset
   Name: vaultstadio
   ```

2. Set Permissions:
   ```
   Datasets → vaultstadio → Permissions
   User: 1000 (or your app user)
   Group: 1000
   Mode: 755
   ```

3. Install with Helm:
   ```bash
   helm install vaultstadio ./helm/vaultstadio \
     --set backend.persistence.hostPath.enabled=true \
     --set backend.persistence.hostPath.path=/mnt/pool/vaultstadio
   ```

### Option 2: PVC (Persistent Volume Claim)

Use Kubernetes storage:

```yaml
# Helm values.yaml
backend:
  persistence:
    enabled: true
    size: 100Gi
    storageClassName: "local-path"
```

## Docker Compose Configuration

### Local Storage

```yaml
# docker-compose.yml
services:
  backend:
    volumes:
      - ./storage:/data/storage
      - ./plugins:/data/plugins
```

### External Storage

```yaml
# docker-compose.yml
services:
  backend:
    volumes:
      - /mnt/nas/vaultstadio:/data/storage:rw
      - vaultstadio_plugins:/data/plugins

volumes:
  vaultstadio_plugins:
```

## Storage Layout

### File Organization

Files are stored with UUIDs to avoid naming conflicts:

```
/data/storage/
├── a1b2c3d4-e5f6-7890-abcd-ef1234567890  # File content
├── b2c3d4e5-f6a7-8901-bcde-f12345678901
└── ...
```

File metadata (name, path, MIME type) is stored in PostgreSQL.

### Plugin Data

Each plugin has its own directory:

```
/data/plugins/
├── com.vaultstadio.image-metadata/
│   └── thumbnails/
├── com.vaultstadio.fulltext-search/
│   └── index/
└── com.vaultstadio.ai-integration/
    └── cache/
```

## Performance Considerations

### For Large Deployments

1. **Use SSD for database**: PostgreSQL benefits from fast storage
2. **Use HDD for files**: File storage can use slower, larger disks
3. **Separate plugin data**: Keep thumbnails/indexes on fast storage

Example TrueNAS setup:
```
apps-pool (SSD)
├── postgresql/     # Database
└── plugins/        # Plugin data

storage-pool (HDD)
└── vaultstadio/    # File storage
```

### Volume Configuration

```yaml
# Helm values for split storage
backend:
  persistence:
    hostPath:
      enabled: true
      path: /mnt/storage-pool/vaultstadio  # HDD
  
  pluginStorage:
    enabled: true
    hostPath:
      enabled: true
      path: /mnt/apps-pool/vaultstadio-plugins  # SSD

postgresql:
  primary:
    persistence:
      storageClass: "fast-storage"  # SSD storage class
```

## Backup Recommendations

### Database Backup

```bash
# Backup
kubectl exec -it postgresql-pod -- pg_dump -U vaultstadio vaultstadio > backup.sql

# Restore
kubectl exec -i postgresql-pod -- psql -U vaultstadio vaultstadio < backup.sql
```

### File Backup

Use TrueNAS replication or snapshots:

1. **Snapshots**: Storage → Snapshots → Add
2. **Replication**: Tasks → Replication Tasks

Or manual backup:
```bash
rsync -av /mnt/pool/vaultstadio/ /backup/vaultstadio/
```

## Troubleshooting

### Permission Denied

```bash
# Check permissions
ls -la /data/storage

# Fix permissions
chown -R 1000:1000 /data/storage
chmod -R 755 /data/storage
```

### Disk Full

```bash
# Check usage
du -sh /data/storage/*

# Find large files
find /data/storage -type f -size +100M
```

### Slow Performance

1. Check disk I/O: `iostat -x 1`
2. Check for network storage issues
3. Consider moving to faster storage
4. Enable caching for frequently accessed files
