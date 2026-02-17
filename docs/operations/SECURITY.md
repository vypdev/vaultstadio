# VaultStadio Security Guide

This document covers security best practices for deploying and operating VaultStadio.

## Overview

VaultStadio is designed with security in mind, implementing industry-standard practices for authentication, authorization, and data protection.

## Authentication

### JWT Tokens

VaultStadio uses JSON Web Tokens (JWT) for authentication.

**Configuration:**

```yaml
# docker-compose.yml or environment variables
JWT_SECRET: "your-256-bit-secret-key-here"
JWT_EXPIRATION: "1h"
REFRESH_TOKEN_EXPIRATION: "7d"
```

**Best Practices:**

1. **Generate strong secrets:**
   ```bash
   # Generate a 256-bit secret
   openssl rand -base64 32
   ```

2. **Rotate secrets periodically** - Update `JWT_SECRET` and invalidate all existing tokens

3. **Use short-lived access tokens** - Default 1 hour is recommended

4. **Token rotation** - Refresh tokens are rotated on each use

### Password Security

VaultStadio uses BCrypt for password hashing.

**Configuration:**

```yaml
BCRYPT_COST: 12  # Recommended minimum
```

**Password Requirements:**
- Minimum 8 characters (configurable)
- No maximum length limit
- All characters allowed

### Session Management

- Sessions are stored server-side
- Logout invalidates all tokens for the session
- "Logout all devices" option available

---

## Authorization

### Role-Based Access Control (RBAC)

VaultStadio supports the following roles:

| Role | Permissions |
|------|-------------|
| `user` | Manage own files, create shares |
| `admin` | All user permissions + user management, system configuration |

### File Permissions

- **Private**: Only owner can access
- **Shared**: Accessible via share link
- **Public**: Accessible without authentication (if enabled)

### API Key Authentication

For automated access:

```bash
# Generate API key
curl -X POST /api/v1/users/me/api-keys \
  -H "Authorization: Bearer <token>" \
  -d '{"name": "Backup Script", "permissions": ["read"]}'
```

---

## Data Protection

### Encryption at Rest

**Storage Backend Options:**

1. **Local filesystem with disk encryption:**
   ```bash
   # Linux - LUKS encryption
   cryptsetup luksFormat /dev/sdb
   cryptsetup open /dev/sdb encrypted_storage
   ```

2. **S3/MinIO with server-side encryption:**
   ```yaml
   S3_SERVER_SIDE_ENCRYPTION: "AES256"
   ```

### Encryption in Transit

**Always use HTTPS in production:**

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    
    ssl_certificate /etc/ssl/certs/vaultstadio.crt;
    ssl_certificate_key /etc/ssl/private/vaultstadio.key;
    
    # Modern TLS configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;
}
```

### Database Security

**PostgreSQL best practices:**

```sql
-- Create dedicated user with minimal permissions
CREATE USER vaultstadio WITH PASSWORD 'strong-password';
GRANT CONNECT ON DATABASE vaultstadio TO vaultstadio;
GRANT USAGE ON SCHEMA public TO vaultstadio;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO vaultstadio;
```

**Connection encryption:**

```yaml
DATABASE_URL: "postgresql://user:pass@host:5432/db?sslmode=require"
```

---

## Network Security

### Firewall Configuration

```bash
# Allow only necessary ports
ufw allow 22/tcp   # SSH
ufw allow 80/tcp   # HTTP (redirect to HTTPS)
ufw allow 443/tcp  # HTTPS
ufw default deny incoming
ufw enable
```

### Rate Limiting

VaultStadio implements rate limiting:

```yaml
RATE_LIMIT_ENABLED: true
RATE_LIMIT_AUTHENTICATED: 1000  # requests per hour
RATE_LIMIT_UNAUTHENTICATED: 100 # requests per hour
```

### CORS Configuration

```yaml
CORS_ALLOWED_ORIGINS: "https://your-domain.com"
CORS_ALLOWED_METHODS: "GET,POST,PUT,DELETE,PATCH"
CORS_ALLOWED_HEADERS: "Authorization,Content-Type"
```

---

## Security Headers

VaultStadio sets the following security headers:

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
```

**Configure in nginx:**

```nginx
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

---

## Audit Logging

VaultStadio logs security-relevant events:

### Logged Events

- Authentication attempts (success/failure)
- Password changes
- File access (downloads, uploads)
- Share creation/access
- Admin actions
- API key usage

### Log Configuration

```yaml
LOG_LEVEL: INFO
LOG_FORMAT: JSON
LOG_SECURITY_EVENTS: true
AUDIT_LOG_PATH: /var/log/vaultstadio/audit.log
```

### Log Retention

```bash
# logrotate configuration
/var/log/vaultstadio/*.log {
    daily
    rotate 90
    compress
    delaycompress
    notifempty
    create 0640 vaultstadio vaultstadio
}
```

---

## Vulnerability Management

### Dependency Scanning

```bash
# Scan Kotlin dependencies
./gradlew dependencyCheckAnalyze

# Check for outdated dependencies
./gradlew dependencyUpdates
```

### Container Security

```dockerfile
# Use non-root user
USER 1000:1000

# Use distroless or minimal base image
FROM gcr.io/distroless/java17-debian12

# Read-only filesystem where possible
RUN chmod -R 555 /app
```

### Security Scanning

```bash
# Scan Docker image
trivy image vaultstadio/backend:latest

# Scan for secrets
trufflehog git file://.
```

---

## Incident Response

### Checklist

1. **Identify** - Determine scope and impact
2. **Contain** - Disable affected accounts/endpoints
3. **Eradicate** - Remove threat
4. **Recover** - Restore from backup if needed
5. **Learn** - Post-incident review

### Emergency Actions

```bash
# Revoke all tokens (force re-login)
curl -X POST /api/v1/admin/security/revoke-all-tokens \
  -H "Authorization: Bearer <admin-token>"

# Disable user account
curl -X POST /api/v1/admin/users/{userId}/disable \
  -H "Authorization: Bearer <admin-token>"

# Block IP address (nginx)
echo "deny 1.2.3.4;" >> /etc/nginx/conf.d/blocked.conf
nginx -s reload
```

---

## Federation Security

### Instance Verification

Federation uses cryptographic verification:

1. **Public key exchange** during federation request
2. **Signed requests** between instances
3. **Certificate pinning** option for trusted instances

### Configuration

```yaml
FEDERATION_ENABLED: true
FEDERATION_REQUIRE_HTTPS: true
FEDERATION_ALLOWED_DOMAINS: "*.trusted-domain.com"
FEDERATION_BLOCKED_DOMAINS: "malicious.com"
```

---

## Backup Security

### Encrypted Backups

```bash
# Encrypt backup with GPG
pg_dump vaultstadio | gpg --symmetric --cipher-algo AES256 -o backup.sql.gpg

# Encrypt with age
pg_dump vaultstadio | age -r age1... > backup.sql.age
```

### Backup Access Control

- Store backups in separate location
- Use separate credentials for backup access
- Rotate backup encryption keys

---

## Compliance

### GDPR Considerations

- **Data export**: `/api/v1/users/me/export`
- **Data deletion**: Complete user data deletion available
- **Consent**: Share links require explicit consent
- **Audit trail**: All access logged

### Data Retention

```yaml
# Configure retention policies
TRASH_RETENTION_DAYS: 30
ACTIVITY_LOG_RETENTION_DAYS: 90
SESSION_RETENTION_DAYS: 7
VERSION_RETENTION_DAYS: 365
```

---

## Security Checklist

### Before Production

- [ ] Change all default passwords
- [ ] Generate strong JWT secret
- [ ] Enable HTTPS with valid certificate
- [ ] Configure firewall rules
- [ ] Enable rate limiting
- [ ] Set up log monitoring
- [ ] Configure backup encryption
- [ ] Review CORS settings
- [ ] Disable debug mode
- [ ] Set secure cookie flags

### Regular Maintenance

- [ ] Review access logs weekly
- [ ] Update dependencies monthly
- [ ] Rotate secrets quarterly
- [ ] Conduct security review annually
- [ ] Test backup restoration
- [ ] Review user access rights

---

## Reporting Security Issues

If you discover a security vulnerability, please report it responsibly:

1. **Do not** open a public issue
2. Email security details to: security@vaultstadio.example.com
3. Include steps to reproduce
4. Allow reasonable time for fix before disclosure

We appreciate responsible disclosure and will acknowledge contributors.
