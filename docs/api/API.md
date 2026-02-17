# VaultStadio API Documentation

**Last updated**: 2026-02-16

## Overview

VaultStadio provides a RESTful API for file storage and management. All endpoints are prefixed with `/api/v1`.

### Endpoints by area

| Area | Section |
|------|---------|
| [Authentication](#authentication) | Login, register, refresh, logout |
| [Storage](#storage-endpoints) | Folders, upload, download, rename, delete, star, trash |
| [Batch operations](#batch-operations-endpoints) | Batch delete, move, copy, star, ZIP, empty trash |
| [Thumbnail & preview](#thumbnail--preview-endpoints) | Thumbnails and file preview |
| [Chunked upload](#chunked-upload-endpoints) | Large file upload |
| [Folder upload](#folder-upload-endpoint) | Folder upload with structure |
| [Search](#search-endpoints) | Search, advanced search, suggestions |
| [Metadata](#metadata-endpoints) | Image, video, document metadata |
| [Share](#share-endpoints) | Share links |
| [User](#user-endpoints) | Profile, password, quota |
| [Admin](#admin-endpoints) | User management, system stats |
| [Plugins](#plugin-endpoints) | List, enable/disable plugins |
| [Health](#health-endpoints) | Health and readiness |
| [Version history](#version-history-endpoints-phase-6) | File versioning (Phase 6) |
| [Sync](#sync-endpoints-phase-6) | Device sync (Phase 6) |
| [Federation](#federation-endpoints-phase-6) | Cross-instance (Phase 6) |
| [Collaboration](#collaboration-endpoints-phase-6) | Real-time collaboration (Phase 6) |
| [AI](#ai-endpoints-phase-6) | Providers, chat, vision, tagging (Phase 6) |
| [WebDAV](#webdav-endpoints-phase-6) | WebDAV protocol (Phase 6) |
| [S3](#s3-compatible-api-phase-6) | S3-compatible API (Phase 6) |
| [Activity](#activity-endpoints) | Activity log |

### Table of contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Authentication Endpoints](#authentication-endpoints)
- [Storage Endpoints](#storage-endpoints)
- [Batch Operations Endpoints](#batch-operations-endpoints)
- [Thumbnail & Preview Endpoints](#thumbnail--preview-endpoints)
- [Chunked Upload Endpoints](#chunked-upload-endpoints)
- [Folder Upload Endpoint](#folder-upload-endpoint)
- [Search Endpoints](#search-endpoints)
- [Metadata Endpoints](#metadata-endpoints)
- [Share Endpoints](#share-endpoints)
- [User Endpoints](#user-endpoints)
- [Admin Endpoints](#admin-endpoints)
- [Plugin Endpoints](#plugin-endpoints)
- [Health Endpoints](#health-endpoints)
- [Error Responses](#error-responses)
- [Rate Limiting](#rate-limiting)
- [Pagination](#pagination)
- [Version History Endpoints (Phase 6)](#version-history-endpoints-phase-6)
- [Sync Endpoints (Phase 6)](#sync-endpoints-phase-6)
- [Federation Endpoints (Phase 6)](#federation-endpoints-phase-6)
- [Collaboration Endpoints (Phase 6)](#collaboration-endpoints-phase-6)
- [AI Endpoints (Phase 6)](#ai-endpoints-phase-6)
- [WebDAV Endpoints (Phase 6)](#webdav-endpoints-phase-6)
- [S3-Compatible API (Phase 6)](#s3-compatible-api-phase-6)
- [Activity Endpoints](#activity-endpoints)

---

**Base URL**: `http://localhost:8080/api/v1`

**Interactive Documentation**: `http://localhost:8080/swagger-ui`

## Authentication

Most endpoints require authentication via JWT token.

### Obtain Token

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user-uuid",
      "email": "user@example.com",
      "username": "username",
      "role": "user"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresAt": "2024-01-08T00:00:00Z"
  }
}
```

### Use Token

Include the token in the Authorization header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Authentication Endpoints

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "username",
  "password": "password123"
}
```

**Response:** `201 Created`
```json
{
  "id": "user-uuid",
  "email": "user@example.com",
  "username": "username",
  "role": "user",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <token>
```

**Response:** `204 No Content`

### Refresh Token

Refresh an expired access token using a valid refresh token. Implements token rotation for security.

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user-uuid",
      "email": "user@example.com",
      "username": "username",
      "role": "user"
    },
    "token": "new-access-token...",
    "refreshToken": "new-refresh-token...",
    "expiresAt": "2024-02-01T00:00:00Z"
  }
}
```

> **Note:** Token rotation is implemented - each refresh invalidates the old refresh token and issues a new pair of tokens.

### Get Current User

Available at both `/api/v1/auth/me` and `/api/v1/user/me` (equivalent endpoints).

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "user-uuid",
  "email": "user@example.com",
  "username": "username",
  "role": "user",
  "quota": {
    "used": 1073741824,
    "total": 10737418240
  }
}
```

---

## Storage Endpoints

### List Folder Contents

```http
GET /api/v1/storage/folder
GET /api/v1/storage/folder/{folderId}
Authorization: Bearer <token>
```

**Query Parameters:**
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `sort` | string | Sort field (name, size, createdAt, updatedAt) | name |
| `order` | string | Sort order (asc, desc) | asc |
| `limit` | int | Items per page | 50 |
| `offset` | int | Pagination offset | 0 |

**Response:**
```json
{
  "items": [
    {
      "id": "item-uuid",
      "name": "Documents",
      "path": "/Documents",
      "type": "folder",
      "parentId": null,
      "size": 0,
      "mimeType": null,
      "isStarred": false,
      "isTrashed": false,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    },
    {
      "id": "item-uuid-2",
      "name": "photo.jpg",
      "path": "/photo.jpg",
      "type": "file",
      "parentId": null,
      "size": 1048576,
      "mimeType": "image/jpeg",
      "isStarred": true,
      "isTrashed": false,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "total": 2,
  "limit": 50,
  "offset": 0
}
```

### Create Folder

```http
POST /api/v1/storage/folder
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "New Folder",
  "parentId": null
}
```

**Response:** `201 Created`
```json
{
  "id": "new-folder-uuid",
  "name": "New Folder",
  "path": "/New Folder",
  "type": "folder",
  ...
}
```

### Upload File

```http
POST /api/v1/storage/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <binary>
parentId: folder-uuid (optional)
```

**Response:** `201 Created`
```json
{
  "id": "file-uuid",
  "name": "uploaded-file.pdf",
  "path": "/uploaded-file.pdf",
  "type": "file",
  "size": 2097152,
  "mimeType": "application/pdf",
  ...
}
```

### Download File

```http
GET /api/v1/storage/download/{itemId}
Authorization: Bearer <token>
```

**Response:** Binary file with appropriate Content-Type header.

### Get Item Details

```http
GET /api/v1/storage/{itemId}
Authorization: Bearer <token>
```

### Update Item (Rename/Move)

```http
PATCH /api/v1/storage/{itemId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "New Name",
  "parentId": "new-parent-folder-uuid"
}
```

### Delete Item

```http
DELETE /api/v1/storage/{itemId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

### Toggle Star

```http
POST /api/v1/storage/{itemId}/star
Authorization: Bearer <token>
```

### Move to Trash

```http
POST /api/v1/storage/{itemId}/trash
Authorization: Bearer <token>
```

### Restore from Trash

```http
POST /api/v1/storage/{itemId}/restore
Authorization: Bearer <token>
```

### Get Starred Items

```http
GET /api/v1/storage/starred
Authorization: Bearer <token>
```

### Get Trash

```http
GET /api/v1/storage/trash
Authorization: Bearer <token>
```

### Get Recent Items

```http
GET /api/v1/storage/recent
Authorization: Bearer <token>
```

### Get Breadcrumbs

```http
GET /api/v1/storage/{itemId}/breadcrumbs
Authorization: Bearer <token>
```

**Response:**
```json
[
  { "id": null, "name": "Home", "path": "/" },
  { "id": "folder-1", "name": "Documents", "path": "/Documents" },
  { "id": "folder-2", "name": "Work", "path": "/Documents/Work" }
]
```

---

## Batch Operations Endpoints

### Batch Delete

Delete multiple items at once.

```http
POST /api/v1/storage/batch/delete
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemIds": ["item-uuid-1", "item-uuid-2", "item-uuid-3"],
  "permanent": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "successful": 3,
    "failed": 0,
    "errors": []
  }
}
```

### Batch Move

Move multiple items to a destination folder.

```http
POST /api/v1/storage/batch/move
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemIds": ["item-uuid-1", "item-uuid-2"],
  "destinationId": "folder-uuid"
}
```

### Batch Copy

Copy multiple items to a destination folder.

```http
POST /api/v1/storage/batch/copy
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemIds": ["item-uuid-1", "item-uuid-2"],
  "destinationId": "folder-uuid"
}
```

### Batch Star

Star or unstar multiple items.

```http
POST /api/v1/storage/batch/star
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemIds": ["item-uuid-1", "item-uuid-2"],
  "starred": true
}
```

### Download as ZIP

Download multiple items as a single ZIP file.

```http
POST /api/v1/storage/batch/download-zip
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemIds": ["item-uuid-1", "item-uuid-2", "item-uuid-3"]
}
```

**Response:** Binary ZIP file with `Content-Type: application/zip`

### Empty Trash

Permanently delete all items in trash.

```http
POST /api/v1/storage/batch/empty-trash
Authorization: Bearer <token>
```

---

## Thumbnail & Preview Endpoints

### Get Thumbnail

Generate and serve a thumbnail for an image file.

```http
GET /api/v1/storage/item/{itemId}/thumbnail?size=medium
Authorization: Bearer <token>
```

**Query Parameters:**
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `size` | string | Thumbnail size (small: 64px, medium: 128px, large: 256px, xlarge: 512px) | medium |

**Response:** PNG image binary

### Get Preview

Get file content for preview (images, text, PDF, video, audio).

```http
GET /api/v1/storage/item/{itemId}/preview
Authorization: Bearer <token>
```

**Supported MIME Types:**
- Images: `image/*`
- Videos: `video/*`
- Audio: `audio/*`
- Text: `text/*`
- PDF: `application/pdf`
- JSON: `application/json`

---

## Chunked Upload Endpoints

For uploading large files (> 100MB).

### Initialize Chunked Upload

```http
POST /api/v1/storage/upload/init
Authorization: Bearer <token>
Content-Type: application/json

{
  "fileName": "large-file.zip",
  "fileSize": 5368709120,
  "mimeType": "application/zip",
  "parentId": "folder-uuid"
}
```

**Response:**
```json
{
  "uploadId": "upload-session-uuid",
  "chunkSize": 10485760,
  "totalChunks": 512
}
```

### Upload Chunk

```http
POST /api/v1/storage/upload/{uploadId}/chunk/{chunkIndex}
Authorization: Bearer <token>
Content-Type: application/octet-stream

<binary chunk data>
```

### Get Upload Status

```http
GET /api/v1/storage/upload/{uploadId}/status
Authorization: Bearer <token>
```

**Response:**
```json
{
  "uploadId": "upload-session-uuid",
  "fileName": "large-file.zip",
  "totalSize": 5368709120,
  "uploadedBytes": 2684354560,
  "progress": 0.5,
  "receivedChunks": [0, 1, 2, 3],
  "missingChunks": [4, 5, 6, 7],
  "isComplete": false
}
```

### Complete Chunked Upload

```http
POST /api/v1/storage/upload/{uploadId}/complete
Authorization: Bearer <token>
```

### Cancel Chunked Upload

```http
DELETE /api/v1/storage/upload/{uploadId}
Authorization: Bearer <token>
```

---

## Folder Upload Endpoint

Upload an entire folder with structure preserved.

```http
POST /api/v1/storage/upload-folder
Authorization: Bearer <token>
Content-Type: multipart/form-data

parentId: folder-uuid (optional)
<relativePath>: <file binary>
<relativePath>: <file binary>
...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uploadedFiles": 25,
    "createdFolders": 5,
    "errors": []
  }
}
```

---

## Search Endpoints

### Search Files

```http
GET /api/v1/search?q=query&limit=50&offset=0
Authorization: Bearer <token>
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `q` | string | Search query (required) |
| `limit` | int | Results limit |
| `offset` | int | Pagination offset |

**Response:**
```json
{
  "items": [...],
  "total": 10,
  "limit": 50,
  "offset": 0
}
```

### Advanced Search

Search with filters for file type, size, and date range.

```http
POST /api/v1/search/advanced
Authorization: Bearer <token>
Content-Type: application/json

{
  "query": "report",
  "searchContent": true,
  "fileTypes": ["pdf", "doc"],
  "minSize": 1024,
  "maxSize": 10485760,
  "fromDate": "2024-01-01",
  "toDate": "2024-12-31",
  "limit": 50,
  "offset": 0
}
```

### Search Suggestions (Autocomplete)

```http
GET /api/v1/search/suggestions?prefix=doc&limit=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": ["document.pdf", "documentation.md", "docker-compose.yml"]
}
```

---

## Metadata Endpoints

### Get All File Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "itemId": "item-uuid",
    "metadata": {
      "width": "1920",
      "height": "1080",
      "cameraMake": "Canon",
      "cameraModel": "EOS R5",
      "dateTaken": "2024-01-15T10:30:00Z"
    },
    "extractedBy": ["com.vaultstadio.plugins.image-metadata"]
  }
}
```

### Get Image Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata/image
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "width": 1920,
    "height": 1080,
    "cameraMake": "Canon",
    "cameraModel": "EOS R5",
    "dateTaken": "2024-01-15T10:30:00Z",
    "aperture": "f/2.8",
    "exposureTime": "1/250",
    "iso": 400,
    "focalLength": "50mm",
    "gpsLatitude": 40.7128,
    "gpsLongitude": -74.0060,
    "colorSpace": "sRGB",
    "bitDepth": 8,
    "keywords": ["landscape", "nature"],
    "copyright": "© 2024 Photographer"
  }
}
```

### Get Video Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata/video
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "width": 3840,
    "height": 2160,
    "duration": 3600,
    "durationFormatted": "1:00:00",
    "videoCodec": "h264",
    "audioCodec": "aac",
    "frameRate": "29.97",
    "bitrate": 15000000,
    "aspectRatio": "16:9",
    "colorSpace": "bt709",
    "isHDR": false,
    "channels": 2,
    "sampleRate": 48000,
    "chapterCount": 5,
    "subtitleTracks": ["en", "es"],
    "audioLanguages": ["en", "es"]
  }
}
```

### Get Document Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata/document
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "title": "Annual Report 2024",
    "author": "John Doe",
    "subject": "Financial Report",
    "keywords": ["finance", "annual", "report"],
    "creator": "Microsoft Word",
    "producer": "Adobe PDF",
    "creationDate": "2024-01-15T09:00:00Z",
    "modificationDate": "2024-01-20T14:30:00Z",
    "pageCount": 50,
    "wordCount": 25000,
    "isIndexed": true,
    "indexedAt": "2024-01-20T15:00:00Z"
  }
}
```

---

## Share Endpoints

### List Shares

```http
GET /api/v1/shares
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "share-uuid",
    "itemId": "file-uuid",
    "itemName": "document.pdf",
    "token": "abc123def456",
    "url": "http://localhost:8080/share/abc123def456",
    "password": null,
    "expiresAt": "2024-02-01T00:00:00Z",
    "maxDownloads": 10,
    "downloadCount": 3,
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

### Create Share

```http
POST /api/v1/shares
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemId": "file-uuid",
  "expirationDays": 7,
  "password": "optional-password",
  "maxDownloads": 10
}
```

**Response:** `201 Created`
```json
{
  "id": "share-uuid",
  "token": "abc123def456",
  "url": "http://localhost:8080/share/abc123def456",
  ...
}
```

### Delete Share

```http
DELETE /api/v1/shares/{shareId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

### Access Shared File (Public)

```http
GET /api/v1/share/{token}
```

If password protected:
```http
POST /api/v1/share/{token}
Content-Type: application/json

{
  "password": "share-password"
}
```

---

## User Endpoints

### Get User Profile

```http
GET /api/v1/users/me
Authorization: Bearer <token>
```

### Update Profile

```http
PATCH /api/v1/users/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "new-username"
}
```

### Change Password

```http
POST /api/v1/users/me/password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

### Get Quota

```http
GET /api/v1/users/me/quota
Authorization: Bearer <token>
```

**Response:**
```json
{
  "used": 1073741824,
  "total": 10737418240,
  "percentage": 10.0
}
```

---

## Admin Endpoints

Requires admin role.

### List Users

```http
GET /api/v1/admin/users
Authorization: Bearer <admin-token>
```

### Create User

```http
POST /api/v1/admin/users
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "username",
  "password": "password123",
  "role": "user",
  "quotaBytes": 10737418240
}
```

### Update User

```http
PATCH /api/v1/admin/users/{userId}
Authorization: Bearer <admin-token>
```

### Delete User

```http
DELETE /api/v1/admin/users/{userId}
Authorization: Bearer <admin-token>
```

### System Stats

```http
GET /api/v1/admin/stats
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "totalUsers": 10,
  "totalFiles": 1000,
  "totalStorage": 10737418240,
  "activeShares": 25
}
```

---

## Plugin Endpoints

### List Plugins

```http
GET /api/v1/plugins
Authorization: Bearer <token>
```

### Get Plugin Details

```http
GET /api/v1/plugins/{pluginId}
Authorization: Bearer <token>
```

### Enable/Disable Plugin (Admin)

```http
POST /api/v1/admin/plugins/{pluginId}/enable
POST /api/v1/admin/plugins/{pluginId}/disable
Authorization: Bearer <admin-token>
```

---

## Health Endpoints

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "version": "2.0.0"
}
```

### Readiness Check

```http
GET /ready
```

**Response:**
```json
{
  "ready": true,
  "checks": {
    "database": {
      "status": "up",
      "latencyMs": 5
    },
    "storage": {
      "status": "up",
      "latencyMs": 2
    }
  }
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": "error_code",
  "message": "Human readable message",
  "details": { ... }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `unauthorized` | 401 | Missing or invalid token |
| `forbidden` | 403 | Insufficient permissions |
| `not_found` | 404 | Resource not found |
| `validation_error` | 400 | Invalid request data |
| `conflict` | 409 | Resource already exists |
| `quota_exceeded` | 413 | Storage quota exceeded |
| `internal_error` | 500 | Server error |

---

## Rate Limiting

API requests are rate limited:

- **Authenticated**: 1000 requests per hour
- **Unauthenticated**: 100 requests per hour

Rate limit headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1609459200
```

---

## Pagination

List endpoints support pagination:

```http
GET /api/v1/storage/folder?limit=20&offset=40
```

Response includes pagination info:
```json
{
  "items": [...],
  "total": 100,
  "limit": 20,
  "offset": 40
}
```

---

## Version History Endpoints (Phase 6)

### Get Version History

```http
GET /api/v1/versions/item/{itemId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "itemId": "file-uuid",
  "currentVersion": 5,
  "versions": [
    {
      "versionNumber": 5,
      "size": 2048576,
      "checksum": "sha256:abc123...",
      "createdBy": "user-uuid",
      "createdAt": "2024-01-15T14:30:00Z",
      "comment": "Final review"
    },
    {
      "versionNumber": 4,
      "size": 2000000,
      "checksum": "sha256:def456...",
      "createdBy": "user-uuid",
      "createdAt": "2024-01-14T10:00:00Z",
      "comment": null
    }
  ]
}
```

### Get Specific Version

```http
GET /api/v1/versions/item/{itemId}/version/{versionNumber}
Authorization: Bearer <token>
```

### Download Version

```http
GET /api/v1/versions/item/{itemId}/version/{versionNumber}/download
Authorization: Bearer <token>
```

**Response:** Binary file content

### Restore Version

```http
POST /api/v1/versions/item/{itemId}/restore
Authorization: Bearer <token>
Content-Type: application/json

{
  "versionNumber": 3,
  "comment": "Restoring previous version"
}
```

### Compare Versions

```http
GET /api/v1/versions/item/{itemId}/diff?from=2&to=5
Authorization: Bearer <token>
```

**Response:**
```json
{
  "fromVersion": 2,
  "toVersion": 5,
  "changes": {
    "sizeChange": 48576,
    "linesAdded": 50,
    "linesRemoved": 10
  }
}
```

### Delete Version

```http
DELETE /api/v1/versions/{versionId}
Authorization: Bearer <token>
```

### Cleanup Versions

Apply retention policy to remove old versions.

```http
POST /api/v1/versions/item/{itemId}/cleanup
Authorization: Bearer <token>
Content-Type: application/json

{
  "maxVersions": 10,
  "maxAgeDays": 90,
  "minKeep": 3
}
```

---

## Sync Endpoints (Phase 6)

### Register Device

```http
POST /api/v1/sync/devices
Authorization: Bearer <token>
Content-Type: application/json

{
  "deviceId": "device-uuid",
  "deviceName": "My MacBook",
  "deviceType": "desktop"
}
```

**Response:**
```json
{
  "id": "device-uuid",
  "name": "My MacBook",
  "type": "desktop",
  "lastSyncAt": null,
  "isActive": true,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### List Devices

```http
GET /api/v1/sync/devices
Authorization: Bearer <token>
```

### Deactivate Device

```http
POST /api/v1/sync/devices/{deviceId}/deactivate
Authorization: Bearer <token>
```

### Remove Device

```http
DELETE /api/v1/sync/devices/{deviceId}
Authorization: Bearer <token>
```

### Pull Changes

Get changes from server since last sync.

```http
POST /api/v1/sync/pull
Authorization: Bearer <token>
Content-Type: application/json

{
  "deviceId": "device-uuid",
  "cursor": "2024-01-15T10:00:00Z"
}
```

**Response:**
```json
{
  "changes": [
    {
      "itemId": "file-uuid",
      "action": "created",
      "item": { ... },
      "timestamp": "2024-01-15T11:00:00Z"
    },
    {
      "itemId": "file-uuid-2",
      "action": "modified",
      "item": { ... },
      "timestamp": "2024-01-15T11:30:00Z"
    }
  ],
  "cursor": "2024-01-15T12:00:00Z",
  "hasMore": false
}
```

### Push Changes

Send local changes to server.

```http
POST /api/v1/sync/push
Authorization: Bearer <token>
Content-Type: application/json

{
  "deviceId": "device-uuid",
  "changes": [
    {
      "itemId": "file-uuid",
      "action": "modified",
      "checksum": "sha256:abc123...",
      "modifiedAt": "2024-01-15T12:00:00Z"
    }
  ]
}
```

### Get Conflicts

```http
GET /api/v1/sync/conflicts
Authorization: Bearer <token>
```

**Response:**
```json
{
  "conflicts": [
    {
      "id": "conflict-uuid",
      "itemId": "file-uuid",
      "localVersion": { ... },
      "remoteVersion": { ... },
      "detectedAt": "2024-01-15T12:00:00Z"
    }
  ]
}
```

### Resolve Conflict

```http
POST /api/v1/sync/conflicts/{conflictId}/resolve
Authorization: Bearer <token>
Content-Type: application/json

{
  "resolution": "keep_local"
}
```

**Resolution options:** `keep_local`, `keep_remote`, `keep_both`

---

## Federation Endpoints (Phase 6)

### Instance Discovery (Public)

```http
GET /api/v1/federation/.well-known/vaultstadio
```

**Response:**
```json
{
  "instanceName": "My VaultStadio",
  "version": "2.0.0",
  "capabilities": ["RECEIVE_SHARES", "SEND_SHARES", "FEDERATED_IDENTITY"],
  "publicKey": "-----BEGIN PUBLIC KEY-----..."
}
```

### Request Federation

```http
POST /api/v1/federation/instances/request
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetDomain": "storage.example.com",
  "message": "Request to federate"
}
```

### List Federated Instances

```http
GET /api/v1/federation/instances
Authorization: Bearer <token>
```

### Block Instance

```http
POST /api/v1/federation/instances/{instanceId}/block
Authorization: Bearer <token>
```

### Remove Instance

```http
DELETE /api/v1/federation/instances/{instanceId}
Authorization: Bearer <token>
```

### Create Federated Share

```http
POST /api/v1/federation/shares
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemId": "file-uuid",
  "targetInstance": "storage.example.com",
  "targetUserId": "user@storage.example.com",
  "permissions": ["READ", "WRITE"]
}
```

### List Outgoing Shares

```http
GET /api/v1/federation/shares/outgoing
Authorization: Bearer <token>
```

### List Incoming Shares

```http
GET /api/v1/federation/shares/incoming?status=PENDING
Authorization: Bearer <token>
```

### Accept Share

```http
POST /api/v1/federation/shares/{shareId}/accept
Authorization: Bearer <token>
```

### Decline Share

```http
POST /api/v1/federation/shares/{shareId}/decline
Authorization: Bearer <token>
```

### Revoke Share

```http
POST /api/v1/federation/shares/{shareId}/revoke
Authorization: Bearer <token>
```

### Link Federated Identity

```http
POST /api/v1/federation/identities
Authorization: Bearer <token>
Content-Type: application/json

{
  "remoteInstance": "storage.example.com",
  "remoteUserId": "user-uuid"
}
```

### List Federated Identities

```http
GET /api/v1/federation/identities
Authorization: Bearer <token>
```

### Unlink Identity

```http
DELETE /api/v1/federation/identities/{identityId}
Authorization: Bearer <token>
```

---

## Collaboration Endpoints (Phase 6)

### Join Session

```http
POST /api/v1/collaboration/sessions/join
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemId": "file-uuid"
}
```

**Response:**
```json
{
  "id": "session-uuid",
  "itemId": "file-uuid",
  "participants": [
    {
      "userId": "user-uuid",
      "displayName": "John Doe",
      "cursorPosition": null,
      "isActive": true
    }
  ],
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### Leave Session

```http
POST /api/v1/collaboration/sessions/{sessionId}/leave
Authorization: Bearer <token>
```

### Get Session

```http
GET /api/v1/collaboration/sessions/{sessionId}
Authorization: Bearer <token>
```

### Get Participants

```http
GET /api/v1/collaboration/sessions/{sessionId}/participants
Authorization: Bearer <token>
```

### Get Document State

```http
GET /api/v1/collaboration/documents/{itemId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "itemId": "file-uuid",
  "version": 15,
  "content": "Document content...",
  "lastModifiedBy": "user-uuid",
  "lastModifiedAt": "2024-01-15T14:30:00Z"
}
```

### Save Document

```http
POST /api/v1/collaboration/documents/{itemId}/save
Authorization: Bearer <token>
```

### Get Comments

```http
GET /api/v1/collaboration/documents/{itemId}/comments
Authorization: Bearer <token>
```

**Response:**
```json
{
  "comments": [
    {
      "id": "comment-uuid",
      "content": "Please review this section",
      "authorId": "user-uuid",
      "authorName": "John Doe",
      "startLine": 10,
      "startColumn": 5,
      "endLine": 10,
      "endColumn": 50,
      "quotedText": "The section text",
      "isResolved": false,
      "replies": [],
      "createdAt": "2024-01-15T10:00:00Z"
    }
  ]
}
```

### Create Comment

```http
POST /api/v1/collaboration/documents/{itemId}/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Please review this section",
  "startLine": 10,
  "startColumn": 5,
  "endLine": 10,
  "endColumn": 50,
  "quotedText": "The section text"
}
```

### Resolve Comment

```http
POST /api/v1/collaboration/documents/{itemId}/comments/{commentId}/resolve
Authorization: Bearer <token>
```

### Reply to Comment

```http
POST /api/v1/collaboration/documents/{itemId}/comments/{commentId}/replies
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Done, please check"
}
```

### Delete Comment

```http
DELETE /api/v1/collaboration/documents/{itemId}/comments/{commentId}
Authorization: Bearer <token>
```

### WebSocket Connection

Real-time updates via WebSocket:

```
ws://localhost:8080/api/v1/collaboration/ws/{sessionId}
Authorization: Bearer <token>
```

**Messages:**
```json
// Cursor update
{
  "type": "cursor",
  "userId": "user-uuid",
  "position": { "line": 10, "column": 5 }
}

// Content change
{
  "type": "operation",
  "operation": {
    "type": "insert",
    "position": 150,
    "text": "new text"
  }
}

// Participant joined
{
  "type": "participant_joined",
  "participant": { ... }
}
```

---

## AI Endpoints (Phase 6)

### List Providers (Admin)

```http
GET /api/v1/ai/providers
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "providers": [
    {
      "type": "ollama",
      "baseUrl": "http://localhost:11434",
      "isActive": true,
      "isAvailable": true
    },
    {
      "type": "openrouter",
      "baseUrl": "https://openrouter.ai/api",
      "isActive": false,
      "isAvailable": true
    }
  ]
}
```

### Configure Provider (Admin)

```http
POST /api/v1/ai/providers
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "type": "ollama",
  "baseUrl": "http://localhost:11434",
  "apiKey": null,
  "model": "llava"
}
```

### Set Active Provider (Admin)

```http
POST /api/v1/ai/providers/active
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "type": "ollama"
}
```

### List Models

```http
GET /api/v1/ai/models
Authorization: Bearer <token>
```

**Response:**
```json
{
  "models": [
    {
      "id": "llava",
      "name": "LLaVA",
      "provider": "ollama",
      "capabilities": ["vision", "chat"]
    }
  ]
}
```

### Chat Completion

```http
POST /api/v1/ai/chat
Authorization: Bearer <token>
Content-Type: application/json

{
  "messages": [
    { "role": "user", "content": "Describe this image" }
  ],
  "model": "llava"
}
```

**Response:**
```json
{
  "content": "This image shows...",
  "model": "llava",
  "usage": {
    "promptTokens": 10,
    "completionTokens": 50
  }
}
```

### Vision (Image Analysis)

```http
POST /api/v1/ai/vision
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "data:image/jpeg;base64,...",
  "prompt": "What is in this image?",
  "mimeType": "image/jpeg"
}
```

### Describe Image

```http
POST /api/v1/ai/describe
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "data:image/jpeg;base64,...",
  "mimeType": "image/jpeg"
}
```

**Response:**
```json
{
  "description": "A sunset over the ocean with..."
}
```

### Auto-Tag Image

```http
POST /api/v1/ai/tag
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "data:image/jpeg;base64,...",
  "mimeType": "image/jpeg"
}
```

**Response:**
```json
{
  "tags": ["sunset", "ocean", "beach", "nature", "landscape"]
}
```

### Classify Content

```http
POST /api/v1/ai/classify
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "This is a financial report...",
  "categories": ["finance", "marketing", "engineering", "hr"]
}
```

**Response:**
```json
{
  "category": "finance",
  "confidence": 0.95
}
```

### Summarize Text

```http
POST /api/v1/ai/summarize
Authorization: Bearer <token>
Content-Type: application/json

{
  "text": "Long document text...",
  "maxLength": 200
}
```

**Response:**
```json
{
  "summary": "This document discusses..."
}
```

---

## WebDAV Endpoints (Phase 6)

VaultStadio provides WebDAV access for desktop integration.

**Base URL:** `http://localhost:8080/webdav`

### Supported Methods

| Method | Description |
|--------|-------------|
| `OPTIONS` | Get supported methods |
| `GET` | Download file |
| `PUT` | Upload/update file |
| `DELETE` | Delete file/folder |
| `MKCOL` | Create folder |
| `COPY` | Copy file/folder |
| `MOVE` | Move/rename file/folder |
| `PROPFIND` | List properties |
| `PROPPATCH` | Update properties |
| `LOCK` | Lock resource |
| `UNLOCK` | Unlock resource |

### Authentication

```
Authorization: Basic base64(email:password)
```

### Example Usage

```bash
# Mount on macOS
mount_webdav http://localhost:8080/webdav /Volumes/VaultStadio

# Mount on Linux
sudo mount -t davfs http://localhost:8080/webdav /mnt/vaultstadio

# Windows
net use Z: http://localhost:8080/webdav /user:email password
```

---

## S3-Compatible API (Phase 6)

VaultStadio provides an S3-compatible API for tools like rclone, s3cmd, etc.

**Base URL:** `http://localhost:8080/s3`

### Supported Operations

| Operation | Endpoint | Description |
|-----------|----------|-------------|
| ListBuckets | `GET /s3` | List root folders as buckets |
| ListObjects | `GET /s3/{bucket}` | List objects in bucket |
| HeadBucket | `HEAD /s3/{bucket}` | Check bucket exists |
| CreateBucket | `PUT /s3/{bucket}` | Create bucket (folder) |
| DeleteBucket | `DELETE /s3/{bucket}` | Delete empty bucket |
| GetObject | `GET /s3/{bucket}/{key}` | Download object |
| HeadObject | `HEAD /s3/{bucket}/{key}` | Get object metadata |
| PutObject | `PUT /s3/{bucket}/{key}` | Upload object |
| DeleteObject | `DELETE /s3/{bucket}/{key}` | Delete object |

### Authentication

AWS Signature Version 4:

```
Authorization: AWS4-HMAC-SHA256 Credential=...
```

Credentials:
- **Access Key ID:** Your email
- **Secret Access Key:** Your password or API key

### Example Usage

```bash
# rclone configuration
rclone config create vaultstadio s3 \
  provider=Other \
  endpoint=http://localhost:8080/s3 \
  access_key_id=user@example.com \
  secret_access_key=your-password

# s3cmd configuration
s3cmd --configure \
  --host=localhost:8080 \
  --host-bucket="%(bucket)s.localhost:8080" \
  --access_key=user@example.com \
  --secret_key=your-password
```

---

## Activity Endpoints

### Get Recent Activity

```http
GET /api/v1/activity?limit=50
Authorization: Bearer <token>
```

**Response:**
```json
{
  "activities": [
    {
      "id": "activity-uuid",
      "type": "file.uploaded",
      "itemId": "file-uuid",
      "itemName": "document.pdf",
      "userId": "user-uuid",
      "userName": "John Doe",
      "timestamp": "2024-01-15T10:00:00Z",
      "details": {
        "size": 1048576
      }
    }
  ]
}
```

### Get Item Activity

```http
GET /api/v1/activity/item/{itemId}
Authorization: Bearer <token>
```

**Activity Types:**
- `file.uploaded`
- `file.downloaded`
- `file.deleted`
- `file.moved`
- `file.copied`
- `file.renamed`
- `file.starred`
- `file.shared`
- `folder.created`
- `version.created`
- `version.restored`

---

## Webhooks (Planned)

Coming in a future release:

```json
{
  "event": "file.uploaded",
  "timestamp": "2024-01-01T00:00:00Z",
  "data": {
    "itemId": "file-uuid",
    "name": "document.pdf"
  }
}
```
