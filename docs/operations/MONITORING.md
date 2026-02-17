# Monitoring & Observability Guide

Comprehensive monitoring setup for VaultStadio deployments.

---

## Health Checks

### Built-in Health Endpoint

```bash
# Basic health check
curl http://localhost:8080/api/health

# Response
{
    "status": "healthy",
    "version": "1.0.0",
    "uptime": "2d 5h 30m",
    "checks": {
        "database": "ok",
        "storage": "ok",
        "plugins": "ok"
    }
}
```

### Kubernetes Probes

```yaml
# Liveness probe
livenessProbe:
    httpGet:
        path: /api/health
        port: 8080
    initialDelaySeconds: 30
    periodSeconds: 10
    failureThreshold: 3

# Readiness probe
readinessProbe:
    httpGet:
        path: /api/health
        port: 8080
    initialDelaySeconds: 5
    periodSeconds: 5
    failureThreshold: 3
```

### Docker Compose Health Check

```yaml
services:
    backend:
        healthcheck:
            test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
            interval: 30s
            timeout: 10s
            retries: 3
            start_period: 40s
```

---

## Logging

### Log Configuration

```bash
# Environment variables
VAULTSTADIO_LOG_LEVEL=INFO          # DEBUG, INFO, WARN, ERROR
VAULTSTADIO_LOG_FORMAT=json         # json, text
VAULTSTADIO_LOG_FILE=/logs/app.log  # Optional file output
```

### Structured Logging

VaultStadio uses structured JSON logging:

```json
{
    "timestamp": "2026-01-30T10:30:00Z",
    "level": "INFO",
    "logger": "StorageService",
    "message": "File uploaded",
    "context": {
        "userId": "user-123",
        "fileId": "file-456",
        "size": 1048576,
        "duration": 250
    }
}
```

### Log Aggregation with Loki

```yaml
# docker-compose.yml
services:
    loki:
        image: grafana/loki:2.9.0
        ports:
            - "3100:3100"
        volumes:
            - loki-data:/loki

    promtail:
        image: grafana/promtail:2.9.0
        volumes:
            - /var/log:/var/log
            - ./promtail-config.yml:/etc/promtail/config.yml
```

```yaml
# promtail-config.yml
server:
    http_listen_port: 9080

positions:
    filename: /tmp/positions.yaml

clients:
    - url: http://loki:3100/loki/api/v1/push

scrape_configs:
    - job_name: vaultstadio
      static_configs:
          - targets:
                - localhost
            labels:
                job: vaultstadio
                __path__: /var/log/vaultstadio/*.log
```

---

## Metrics

### Prometheus Integration

Enable metrics endpoint:

```bash
VAULTSTADIO_METRICS_ENABLED=true
VAULTSTADIO_METRICS_PATH=/metrics
```

### Key Metrics

```prometheus
# HTTP metrics
http_requests_total{method="GET", path="/api/storage/folder", status="200"}
http_request_duration_seconds{method="GET", path="/api/storage/folder", quantile="0.95"}

# Storage metrics
storage_uploads_total
storage_downloads_total
storage_bytes_uploaded_total
storage_bytes_downloaded_total

# Database metrics
db_connections_active
db_connections_idle
db_query_duration_seconds

# JVM metrics
jvm_memory_used_bytes{area="heap"}
jvm_gc_pause_seconds
jvm_threads_current
```

### Prometheus Configuration

```yaml
# prometheus.yml
global:
    scrape_interval: 15s

scrape_configs:
    - job_name: 'vaultstadio'
      static_configs:
          - targets: ['backend:8080']
      metrics_path: /metrics
```

---

## Dashboards

### Grafana Setup

```yaml
# docker-compose.yml
services:
    grafana:
        image: grafana/grafana:10.0.0
        ports:
            - "3000:3000"
        environment:
            - GF_SECURITY_ADMIN_PASSWORD=admin
        volumes:
            - grafana-data:/var/lib/grafana
            - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
            - ./grafana/datasources:/etc/grafana/provisioning/datasources
```

### Key Dashboard Panels

1. **Overview**
   - Request rate
   - Error rate
   - Response time (p50, p95, p99)
   - Active users

2. **Storage**
   - Upload/download rate
   - Bytes transferred
   - Storage usage
   - File count

3. **Database**
   - Query latency
   - Connection pool usage
   - Active transactions
   - Slow queries

4. **System**
   - CPU usage
   - Memory usage
   - Disk I/O
   - Network I/O

---

## Alerting

### Alertmanager Configuration

```yaml
# alertmanager.yml
route:
    group_by: ['alertname']
    group_wait: 30s
    group_interval: 5m
    repeat_interval: 4h
    receiver: 'default'

receivers:
    - name: 'default'
      slack_configs:
          - api_url: 'https://hooks.slack.com/services/xxx'
            channel: '#alerts'
      email_configs:
          - to: 'ops@example.com'
```

### Alert Rules

```yaml
# alerts.yml
groups:
    - name: vaultstadio
      rules:
          - alert: HighErrorRate
            expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
            for: 5m
            labels:
                severity: critical
            annotations:
                summary: "High error rate detected"

          - alert: SlowResponses
            expr: histogram_quantile(0.95, http_request_duration_seconds) > 1
            for: 5m
            labels:
                severity: warning
            annotations:
                summary: "95th percentile response time > 1s"

          - alert: DatabaseConnectionsHigh
            expr: db_connections_active / db_connections_max > 0.9
            for: 5m
            labels:
                severity: warning
            annotations:
                summary: "Database connection pool near capacity"

          - alert: StorageSpaceLow
            expr: storage_bytes_free / storage_bytes_total < 0.1
            for: 10m
            labels:
                severity: critical
            annotations:
                summary: "Less than 10% storage space remaining"

          - alert: ServiceDown
            expr: up{job="vaultstadio"} == 0
            for: 1m
            labels:
                severity: critical
            annotations:
                summary: "VaultStadio service is down"
```

---

## Distributed Tracing

### Jaeger Integration

```bash
# Environment configuration
VAULTSTADIO_TRACING_ENABLED=true
VAULTSTADIO_TRACING_ENDPOINT=http://jaeger:14268/api/traces
VAULTSTADIO_TRACING_SAMPLE_RATE=0.1
```

```yaml
# docker-compose.yml
services:
    jaeger:
        image: jaegertracing/all-in-one:1.50
        ports:
            - "16686:16686"  # UI
            - "14268:14268"  # HTTP collector
        environment:
            - COLLECTOR_OTLP_ENABLED=true
```

### Trace Context

All requests include trace context:

```
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

---

## Monitoring Stack (Complete)

### Docker Compose

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
    prometheus:
        image: prom/prometheus:v2.47.0
        volumes:
            - ./prometheus.yml:/etc/prometheus/prometheus.yml
            - prometheus-data:/prometheus
        ports:
            - "9090:9090"

    grafana:
        image: grafana/grafana:10.0.0
        volumes:
            - grafana-data:/var/lib/grafana
        ports:
            - "3000:3000"
        depends_on:
            - prometheus

    alertmanager:
        image: prom/alertmanager:v0.26.0
        volumes:
            - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
        ports:
            - "9093:9093"

    loki:
        image: grafana/loki:2.9.0
        ports:
            - "3100:3100"
        volumes:
            - loki-data:/loki

volumes:
    prometheus-data:
    grafana-data:
    loki-data:
```

---

## Best Practices

1. **Set up alerts first** - Don't wait for incidents
2. **Use structured logging** - Enables powerful queries
3. **Track business metrics** - Not just technical metrics
4. **Correlate logs and traces** - Use trace IDs in logs
5. **Review dashboards weekly** - Keep them relevant
6. **Document runbooks** - For each alert type

---

## See Also

- [PERFORMANCE_TUNING.md](PERFORMANCE_TUNING.md) - Performance optimization
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide
- [TROUBLESHOOTING.md](../getting-started/TROUBLESHOOTING.md) - Common issues
