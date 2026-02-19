# Docker Build Guide

## Overview

VaultStadio uses multi-stage Docker builds for optimized images:

- **Backend**: Kotlin/Ktor compiled with Gradle
- **Frontend**: Compose Multiplatform for Web (WASM)

## Building Images

### Build All Services

```bash
docker-compose -f docker/docker-compose.yml build
```

### Build Individual Services

```bash
# Backend only
docker build -f docker/Dockerfile.backend -t vaultstadio-backend:latest .

# Frontend only
docker build -f docker/Dockerfile.frontend -t vaultstadio-frontend:latest .
```

## Backend Image

### Build Stages

1. **Builder Stage** (gradle:8.10-jdk17):
   - Copies Gradle configuration and `backend/` source code
   - Runs `gradle :backend:api:installDist`

2. **Runtime Stage** (eclipse-temurin:21-jre-alpine):
   - Minimal JRE image
   - Copies compiled application
   - Creates non-root user
   - Sets up storage directories

### Dockerfile

```dockerfile
# Stage 1: Build
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY backend ./backend
RUN gradle :backend:api:installDist --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r vaultstadio && useradd -r -g vaultstadio vaultstadio
COPY --from=builder /app/backend/api/build/install/api /app
RUN mkdir -p /data/storage /data/plugins
USER vaultstadio
EXPOSE 8080
CMD ["./bin/api"]
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/vaultstadio` |
| `DATABASE_USER` | Database username | `vaultstadio` |
| `DATABASE_PASSWORD` | Database password | `vaultstadio` |
| `VAULTSTADIO_STORAGE_PATH` | File storage path | `/data/storage` |
| `JAVA_OPTS` | JVM options | `-Xms256m -Xmx1024m` |

## Frontend Image

### Build Stages

1. **Builder Stage** (gradle:8.10-jdk17):
   - Copies `frontend/` (standalone Gradle project)
   - Runs `gradle -p frontend :composeApp:wasmJsBrowserDistribution`

2. **Runtime Stage** (nginx:alpine):
   - Copies compiled WASM files from `frontend/composeApp/build/dist/wasmJs/productionExecutable`
   - Serves via Nginx
   - Proxies API requests to backend

### Dockerfile

```dockerfile
# Stage 1: Build
FROM gradle:8.10-jdk17 AS builder
WORKDIR /app
COPY frontend ./frontend
RUN gradle -p frontend :composeApp:wasmJsBrowserDistribution --no-daemon

# Stage 2: Serve
FROM nginx:alpine
COPY --from=builder /app/frontend/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Image Sizes

| Image | Size |
|-------|------|
| Backend | ~180MB |
| Frontend | ~30MB |
| PostgreSQL | ~230MB |

## Development

### Run with Docker Compose

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# View logs
docker-compose -f docker/docker-compose.yml logs -f

# Rebuild and restart
docker-compose -f docker/docker-compose.yml up --build -d

# Stop all services
docker-compose -f docker/docker-compose.yml down
```

### Access Services

- Frontend: http://localhost
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

## CI/CD Integration

### GitHub Actions

```yaml
- name: Build Docker images
  run: |
    docker build -f docker/Dockerfile.backend \
      -t vaultstadio-backend:${{ github.sha }} .
    docker build -f docker/Dockerfile.frontend \
      -t vaultstadio-frontend:${{ github.sha }} .
```

## Troubleshooting

### Build Fails on Gradle

**Issue**: Gradle build fails

**Solutions**:
1. Ensure Docker has enough memory (at least 4GB)
2. Check if all source files are present
3. Try building locally first: `./gradlew :backend:api:build`

### Frontend Build Fails

**Issue**: WASM compilation fails

**Solutions**:
1. Ensure JDK 21+ is being used
2. Check Gradle memory settings
3. Try building the frontend locally: `cd frontend && ./gradlew :composeApp:compileKotlinDesktop`

### Container Won't Start

**Issue**: Container exits immediately

**Solutions**:
1. Check logs: `docker logs vaultstadio-backend`
2. Verify environment variables are set
3. Check database connectivity
