# High Availability Guide

Strategies for running VaultStadio in high-availability configurations.

---

## Overview

VaultStadio can be deployed in HA configurations for:

- **Zero-downtime deployments**
- **Automatic failover**
- **Horizontal scaling**
- **Geographic distribution**

---

## Architecture Patterns

### Single Instance (Development)

```
┌─────────────────┐
│   VaultStadio   │
│    (Backend)    │
└────────┬────────┘
         │
    ┌────▼────┐
    │ PostgreSQL│
    └────┬────┘
         │
    ┌────▼────┐
    │ Storage │
    │ (Local) │
    └─────────┘
```

### Active-Passive (Basic HA)

```
┌─────────────────┐     ┌─────────────────┐
│   VaultStadio   │     │   VaultStadio   │
│    (Primary)    │     │   (Standby)     │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └───────────┬───────────┘
                     │
            ┌────────▼────────┐
            │  Load Balancer  │
            └────────┬────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │PostgreSQL│ │PostgreSQL│ │  S3/    │
    │ Primary │ │ Replica │ │  MinIO  │
    └─────────┘ └─────────┘ └─────────┘
```

### Active-Active (Full HA)

```
                    ┌─────────────────┐
                    │  DNS / CDN      │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼────┐  ┌──────▼───────┐  ┌───▼──────────┐
     │  Region A   │  │   Region B   │  │   Region C   │
     │ VaultStadio │  │  VaultStadio │  │  VaultStadio │
     └──────┬──────┘  └──────┬───────┘  └──────┬───────┘
            │                │                 │
     ┌──────▼──────┐  ┌──────▼───────┐  ┌──────▼───────┐
     │  PostgreSQL │  │  PostgreSQL  │  │  PostgreSQL  │
     │   Primary   │◄─┤   Replica    │◄─┤   Replica    │
     └─────────────┘  └──────────────┘  └──────────────┘
            │
     ┌──────▼──────────────────────────────────────────┐
     │              S3 / MinIO (Replicated)            │
     └─────────────────────────────────────────────────┘
```

---

## Load Balancing

### Nginx Configuration

```nginx
# /etc/nginx/nginx.conf
upstream vaultstadio {
    least_conn;
    server backend1:8080 weight=5;
    server backend2:8080 weight=5;
    server backend3:8080 backup;
    
    keepalive 32;
}

server {
    listen 80;
    listen 443 ssl http2;
    
    ssl_certificate /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;
    
    location / {
        proxy_pass http://vaultstadio;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Health checks
        health_check interval=5s fails=3 passes=2;
    }
    
    # WebSocket support
    location /api/ws {
        proxy_pass http://vaultstadio;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### HAProxy Configuration

```haproxy
# /etc/haproxy/haproxy.cfg
frontend http_front
    bind *:80
    bind *:443 ssl crt /etc/ssl/cert.pem
    default_backend vaultstadio

backend vaultstadio
    balance roundrobin
    option httpchk GET /api/health
    http-check expect status 200
    
    server backend1 192.168.1.10:8080 check inter 5s fall 3 rise 2
    server backend2 192.168.1.11:8080 check inter 5s fall 3 rise 2
    server backend3 192.168.1.12:8080 check inter 5s fall 3 rise 2 backup
```

### Kubernetes Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
    name: vaultstadio
    annotations:
        nginx.ingress.kubernetes.io/proxy-body-size: "10g"
        nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
spec:
    ingressClassName: nginx
    tls:
        - hosts:
              - storage.example.com
          secretName: tls-secret
    rules:
        - host: storage.example.com
          http:
              paths:
                  - path: /
                    pathType: Prefix
                    backend:
                        service:
                            name: vaultstadio
                            port:
                                number: 8080
```

---

## Database HA

### PostgreSQL Streaming Replication

**Primary Configuration:**

```ini
# postgresql.conf
wal_level = replica
max_wal_senders = 5
wal_keep_size = 1GB
synchronous_commit = on
synchronous_standby_names = 'replica1'
```

**Replica Configuration:**

```ini
# postgresql.conf
hot_standby = on
```

```bash
# recovery.conf / postgresql.auto.conf
primary_conninfo = 'host=primary port=5432 user=replicator password=secret'
```

### Patroni (Recommended)

```yaml
# patroni.yml
scope: vaultstadio
namespace: /db/
name: postgresql0

restapi:
    listen: 0.0.0.0:8008
    connect_address: 192.168.1.10:8008

etcd:
    hosts: etcd1:2379,etcd2:2379,etcd3:2379

bootstrap:
    dcs:
        ttl: 30
        loop_wait: 10
        retry_timeout: 10
        maximum_lag_on_failover: 1048576
    pg_hba:
        - host replication replicator 0.0.0.0/0 md5
        - host all all 0.0.0.0/0 md5

postgresql:
    listen: 0.0.0.0:5432
    connect_address: 192.168.1.10:5432
    data_dir: /data/patroni
    authentication:
        replication:
            username: replicator
            password: secret
        superuser:
            username: postgres
            password: secret
```

---

## Storage HA

### MinIO Distributed Mode

```yaml
# docker-compose.yml
services:
    minio1:
        image: minio/minio
        command: server http://minio{1...4}/data --console-address ":9001"
        environment:
            MINIO_ROOT_USER: admin
            MINIO_ROOT_PASSWORD: password
        volumes:
            - minio1-data:/data

    minio2:
        image: minio/minio
        command: server http://minio{1...4}/data --console-address ":9001"
        volumes:
            - minio2-data:/data

    minio3:
        image: minio/minio
        command: server http://minio{1...4}/data --console-address ":9001"
        volumes:
            - minio3-data:/data

    minio4:
        image: minio/minio
        command: server http://minio{1...4}/data --console-address ":9001"
        volumes:
            - minio4-data:/data
```

### S3 Cross-Region Replication

```bash
# Enable versioning
aws s3api put-bucket-versioning \
    --bucket vaultstadio-primary \
    --versioning-configuration Status=Enabled

# Configure replication
aws s3api put-bucket-replication \
    --bucket vaultstadio-primary \
    --replication-configuration file://replication.json
```

---

## Session Management

### Stateless Sessions

VaultStadio uses JWT tokens for stateless authentication, enabling horizontal scaling without session affinity.

```bash
# JWT configuration (must be same across all instances)
VAULTSTADIO_JWT_SECRET=shared-secret-across-all-instances
```

### Redis for Shared State (Optional)

```yaml
services:
    redis:
        image: redis:7-alpine
        command: redis-server --appendonly yes
        volumes:
            - redis-data:/data

    redis-sentinel:
        image: redis:7-alpine
        command: redis-sentinel /etc/redis/sentinel.conf
```

```bash
VAULTSTADIO_SESSION_STORE=redis
VAULTSTADIO_REDIS_SENTINEL_MASTER=mymaster
VAULTSTADIO_REDIS_SENTINEL_NODES=sentinel1:26379,sentinel2:26379,sentinel3:26379
```

---

## Kubernetes Deployment

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
    name: vaultstadio
spec:
    scaleTargetRef:
        apiVersion: apps/v1
        kind: Deployment
        name: vaultstadio
    minReplicas: 2
    maxReplicas: 10
    metrics:
        - type: Resource
          resource:
              name: cpu
              target:
                  type: Utilization
                  averageUtilization: 70
        - type: Resource
          resource:
              name: memory
              target:
                  type: Utilization
                  averageUtilization: 80
```

### Pod Disruption Budget

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
    name: vaultstadio
spec:
    minAvailable: 1
    selector:
        matchLabels:
            app: vaultstadio
```

### Anti-Affinity Rules

```yaml
affinity:
    podAntiAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                  labelSelector:
                      matchLabels:
                          app: vaultstadio
                  topologyKey: kubernetes.io/hostname
```

---

## Disaster Recovery

### Failover Procedures

1. **Automatic Failover (Patroni/HAProxy)**
   - Load balancer detects backend failure
   - Traffic redirected to healthy instances
   - Database promotes replica if needed

2. **Manual Failover**
   ```bash
   # Promote PostgreSQL replica
   pg_ctl promote -D /var/lib/postgresql/data
   
   # Update DNS/load balancer
   # Restart application with new primary
   ```

### Recovery Time Objectives

| Component | RTO | RPO |
|-----------|-----|-----|
| Application | < 1 min | 0 |
| Database | < 5 min | < 1 min |
| Storage | < 10 min | 0 |

---

## Monitoring for HA

### Key HA Metrics

- Replica lag
- Failover events
- Load balancer health checks
- Connection distribution
- Response time per instance

### Alerts

```yaml
- alert: ReplicaLagHigh
  expr: pg_replication_lag_seconds > 30
  for: 5m
  labels:
      severity: warning

- alert: InstanceDown
  expr: up{job="vaultstadio"} == 0
  for: 1m
  labels:
      severity: critical

- alert: AllReplicasDown
  expr: count(up{job="vaultstadio"} == 1) < 2
  for: 1m
  labels:
      severity: critical
```

---

## See Also

- [DEPLOYMENT.md](DEPLOYMENT.md) - Basic deployment
- [BACKUP_RESTORE.md](BACKUP_RESTORE.md) - Backup strategies
- [MONITORING.md](MONITORING.md) - Monitoring setup
- [PERFORMANCE_TUNING.md](PERFORMANCE_TUNING.md) - Performance optimization
