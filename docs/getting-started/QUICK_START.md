# Quick Start Guide

Get VaultStadio up and running in 5 minutes.

## Prerequisites

- Docker and Docker Compose installed
- OR JDK 21+ and Gradle 8+ for development

## Option 1: Docker Compose (Easiest)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/vaultstadio.git
cd vaultstadio
```

### 2. Configure Environment

```bash
# Copy example environment file
cp docker/.env.example docker/.env

# Edit the file (optional - defaults work for local testing)
# nano docker/.env
```

**Minimum configuration** (in `docker/.env`):
```env
POSTGRES_PASSWORD=your-secure-password
VAULTSTADIO_JWT_SECRET=your-32-character-secret-key-here
```

### 3. Start Services

```bash
docker-compose -f docker/docker-compose.yml up -d
```

### 4. Access the Application

- **Web UI**: http://localhost
- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui

### 5. Create Your Account

1. Open http://localhost
2. Click "Register"
3. Enter your email, username, and password
4. Start uploading files!

---

## Option 2: Development Setup

### 1. Clone and Setup

```bash
git clone https://github.com/yourusername/vaultstadio.git
cd vaultstadio
```

### 2. Start PostgreSQL

```bash
docker-compose -f docker/docker-compose.yml up -d postgres
```

### 3. Run the Backend

```bash
./gradlew :backend:api:run
```

The API will be available at http://localhost:8080

### 4. Run the Frontend

Frontend is a **standalone** project in `frontend/`. From repo root you can use Make, or run Gradle from `frontend/`:

**Desktop App:**
```bash
make desktop-run
# Or: cd frontend && ./gradlew :composeApp:run
```

**Web (WASM) – development dev server:**
```bash
make frontend-run
# Or: cd frontend && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**Web (WASM) – production dev server:**
```bash
make frontend-run-prod
# Or: cd frontend && ./gradlew :composeApp:wasmJsBrowserProductionRun
```

---

## Option 3: TrueNAS Scale

### 1. Build Docker Images

```bash
docker-compose -f docker/docker-compose.yml build

# Push to your registry
docker tag vaultstadio-backend:latest your-registry/vaultstadio-backend:latest
docker tag vaultstadio-frontend:latest your-registry/vaultstadio-frontend:latest
docker push your-registry/vaultstadio-backend:latest
docker push your-registry/vaultstadio-frontend:latest
```

### 2. Install via Helm

```bash
helm install vaultstadio ./helm/vaultstadio \
  --set backend.image.repository=your-registry/vaultstadio-backend \
  --set frontend.image.repository=your-registry/vaultstadio-frontend \
  --set backend.persistence.hostPath.enabled=true \
  --set backend.persistence.hostPath.path=/mnt/pool/vaultstadio \
  --set backend.env.VAULTSTADIO_JWT_SECRET="your-32-char-secret" \
  --set postgresql.auth.password="secure-password"
```

---

## First Steps After Installation

### 1. Create Admin User

The first user registered becomes the admin (or use API):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "username": "admin",
    "password": "secure-password"
  }'
```

### 2. Upload Your First File

**Via Web UI:**
1. Log in
2. Click "Upload" button
3. Select files
4. Done!

**Via API:**
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"secure-password"}' \
  | jq -r '.accessToken')

# Upload file
curl -X POST http://localhost:8080/api/v1/storage/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/file.pdf"
```

### 3. Create a Folder

```bash
curl -X POST http://localhost:8080/api/v1/storage/folder \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Documents"}'
```

### 4. Share a File

```bash
curl -X POST http://localhost:8080/api/v1/shares \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "file-uuid",
    "expirationDays": 7,
    "maxDownloads": 10
  }'
```

---

## Common Commands

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# Stop all services
docker-compose -f docker/docker-compose.yml down

# View logs
docker-compose -f docker/docker-compose.yml logs -f

# Rebuild after changes
docker-compose -f docker/docker-compose.yml up --build -d

# Run tests
./gradlew test

# Build for production
make build
```

---

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose -f docker/docker-compose.yml logs backend

# Common issues:
# - Database not ready: wait a few seconds and restart
# - Port in use: change BACKEND_PORT in .env
```

### Database connection error

```bash
# Ensure PostgreSQL is running
docker-compose -f docker/docker-compose.yml ps

# Check database logs
docker-compose -f docker/docker-compose.yml logs postgres
```

### Can't access web UI

1. Check if containers are running: `docker ps`
2. Check nginx logs: `docker-compose logs frontend`
3. Verify port 80 is not in use

---

## Phase 6: Advanced Features

VaultStadio includes advanced features for enterprise use cases:

### Mount as Network Drive (WebDAV)

**macOS:**
```bash
mount_webdav http://localhost:8080/webdav /Volumes/VaultStadio
```

**Windows:**
```cmd
net use Z: http://localhost:8080/webdav /user:username password
```

**Linux:**
```bash
sudo mount -t davfs http://localhost:8080/webdav /mnt/vaultstadio
```

### Use with AWS CLI (S3 Compatible)

```bash
# Configure AWS CLI
aws configure
# Access Key: your-api-key
# Secret Key: your-api-secret
# Region: us-east-1

# List files
aws s3 ls --endpoint-url http://localhost:8080/s3

# Upload file
aws s3 cp myfile.txt s3://documents/myfile.txt --endpoint-url http://localhost:8080/s3
```

### Multi-Device Sync

Register a sync device and keep files synchronized:

```bash
# Register device
curl -X POST http://localhost:8080/api/v1/sync/devices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "my-laptop", "deviceName": "My Laptop", "deviceType": "DESKTOP"}'

# Pull changes
curl -X POST http://localhost:8080/api/v1/sync/pull \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Device-ID: my-laptop" \
  -H "Content-Type: application/json" \
  -d '{"limit": 100}'
```

See [Phase 6 Advanced Features](PHASE6_ADVANCED_FEATURES.md) for complete documentation.

---

## Next Steps

- Read the [API Documentation](../api/API.md)
- Explore [Phase 6 Advanced Features](PHASE6_ADVANCED_FEATURES.md) (WebDAV, S3, Sync, Federation)
- Configure [Storage Options](../operations/STORAGE_CONFIGURATION.md)
- Learn about [Plugin Development](../plugins/PLUGIN_DEVELOPMENT.md)
- Set up [Production Deployment](../operations/DEPLOYMENT.md)
