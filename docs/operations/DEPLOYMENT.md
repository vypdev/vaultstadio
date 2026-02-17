# Deployment Guide

## Prerequisites

- TrueNAS SCALE 23.10 or later
- Kubernetes cluster (included in TrueNAS SCALE)
- Helm 3.x (included in TrueNAS SCALE)
- Docker (for building images)

## Deployment Methods

### Method 1: TrueNAS SCALE App Catalog (Recommended)

1. **Build and Push Docker Images**:
   ```bash
   # Build images
   docker-compose -f docker/docker-compose.yml build
   
   # Tag and push to registry
   docker tag vaultstadio-backend:latest your-registry/vaultstadio-backend:2.0.0
   docker tag vaultstadio-frontend:latest your-registry/vaultstadio-frontend:2.0.0
   docker push your-registry/vaultstadio-backend:2.0.0
   docker push your-registry/vaultstadio-frontend:2.0.0
   ```

2. **Package Helm Chart**:
   ```bash
   helm package helm/vaultstadio
   ```

3. **Create Custom Catalog**:
   - Create a GitHub repository for your catalog
   - Add the packaged chart
   - Structure:
     ```
     catalog-repo/
     └── charts/
         └── vaultstadio/
             ├── Chart.yaml
             ├── values.yaml
             ├── questions.yaml
             └── templates/
     ```

4. **Add Catalog in TrueNAS**:
   - Navigate to **Apps → Discover Apps**
   - Click **Manage Catalogs**
   - Click **Add Catalog**
   - Enter:
     - **Name**: VaultStadio
     - **Repository**: Your GitHub repository URL
     - **Branch**: main
   - Click **Save**

5. **Install Application**:
   - Navigate to **Apps → Available Applications**
   - Search for "VaultStadio"
   - Click **Install**
   - Configure:
     - **Host Path**: `/mnt/pool/vaultstadio` (your storage path)
     - **JWT Secret**: Generate a secure 32+ character string
     - **Database Password**: Set a secure password
   - Click **Install**

### Method 2: Direct Helm Installation

1. **Install via Helm CLI**:
   ```bash
   helm install vaultstadio ./helm/vaultstadio \
     --set backend.persistence.hostPath.enabled=true \
     --set backend.persistence.hostPath.path=/mnt/pool/vaultstadio \
     --set backend.env.VAULTSTADIO_JWT_SECRET="your-32-char-secret-here" \
     --set postgresql.auth.password="secure-db-password" \
     --set ingress.hosts[0].host="vaultstadio.local"
   ```

2. **Verify Installation**:
   ```bash
   kubectl get pods -l app.kubernetes.io/name=vaultstadio
   kubectl get svc -l app.kubernetes.io/name=vaultstadio
   ```

### Method 3: Docker Compose (Local/Testing)

1. **Configure Environment**:
   ```bash
   cp docker/.env.example docker/.env
   # Edit docker/.env with your settings
   ```

2. **Start Services**:
   ```bash
   docker-compose -f docker/docker-compose.yml up -d
   ```

3. **Access Application**:
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui

## Configuration

### Required Settings

| Setting | Description | Example |
|---------|-------------|---------|
| `backend.env.VAULTSTADIO_JWT_SECRET` | JWT signing secret (32+ chars) | `my-super-secret-key-32-chars!!` |
| `postgresql.auth.password` | Database password | `secure-password` |
| `backend.persistence.hostPath.path` | Storage path on TrueNAS | `/mnt/pool/vaultstadio` |

### Optional Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `ingress.hosts[0].host` | Hostname | `vaultstadio.local` |
| `backend.resources.limits.memory` | Memory limit | `2Gi` |
| `backend.resources.limits.cpu` | CPU limit | `2` |

## Post-Deployment

### 1. Verify Installation

```bash
# Check pods
kubectl get pods -l app.kubernetes.io/name=vaultstadio

# Check services
kubectl get svc -l app.kubernetes.io/name=vaultstadio

# Check logs
kubectl logs -l app.kubernetes.io/component=backend
```

### 2. Access the Application

- **Web UI**: `http://your-truenas-ip` (or configured hostname)
- **API**: `http://your-truenas-ip:8080/api/v1`
- **Swagger**: `http://your-truenas-ip:8080/swagger-ui`

### 3. Create First User

1. Open the web UI
2. Click "Register"
3. Create your admin account

## Upgrading

### Via TrueNAS UI

1. Navigate to **Apps → Installed Applications**
2. Find "VaultStadio"
3. Click **Update** (if available)
4. Review changes and confirm

### Via Helm

```bash
helm upgrade vaultstadio ./helm/vaultstadio \
  --set backend.image.tag=2.0.1
```

## Backup and Restore

### Backup Database

```bash
# Get PostgreSQL pod
PG_POD=$(kubectl get pods -l app.kubernetes.io/component=postgresql -o jsonpath='{.items[0].metadata.name}')

# Create backup
kubectl exec -it $PG_POD -- pg_dump -U vaultstadio vaultstadio > backup.sql
```

### Restore Database

```bash
# Restore from backup
kubectl exec -i $PG_POD -- psql -U vaultstadio vaultstadio < backup.sql
```

### Backup Files

Files are stored in the host path (e.g., `/mnt/pool/vaultstadio`). Use TrueNAS replication or snapshots for backup.

## Uninstallation

### Via TrueNAS UI

1. Navigate to **Apps → Installed Applications**
2. Find "VaultStadio"
3. Click **Delete**
4. Confirm deletion

### Via Helm

```bash
helm uninstall vaultstadio
```

**Note**: This preserves persistent volumes. To delete everything:

```bash
kubectl delete pvc -l app.kubernetes.io/name=vaultstadio
```

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name>

# Check logs
kubectl logs <pod-name>
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
kubectl get pods -l app.kubernetes.io/component=postgresql

# Check connection
kubectl exec -it <backend-pod> -- curl http://localhost:8080/ready
```

### Storage Issues

```bash
# Check PVCs
kubectl get pvc

# Verify host path permissions
ls -la /mnt/pool/vaultstadio
```

## Performance Tuning

### For Large Deployments

```yaml
# values.yaml
backend:
  replicaCount: 2
  resources:
    limits:
      cpu: "4"
      memory: 4Gi
    requests:
      cpu: "1"
      memory: 1Gi

postgresql:
  primary:
    resources:
      limits:
        memory: 2Gi
```

### JVM Tuning

```yaml
backend:
  env:
    JAVA_OPTS: "-Xms512m -Xmx2048m -XX:+UseG1GC"
```
