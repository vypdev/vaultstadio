# VaultStadio Plugin System

## Overview

VaultStadio features an extensible plugin system that allows adding functionality without modifying core code. Plugins can:

- React to file events (upload, download, delete)
- Extract and store metadata
- Generate thumbnails
- Provide full-text search
- Add custom API endpoints

## Built-in Plugins

### Image Metadata Extractor

**ID**: `com.vaultstadio.plugins.image-metadata`

Extracts EXIF, IPTC, and XMP metadata from image files.

#### Supported Formats
- JPEG, PNG, GIF, WebP, TIFF
- HEIC, HEIF
- RAW formats (Canon CR2, Nikon NEF, Sony ARW)

#### Extracted Metadata

| Category | Fields |
|----------|--------|
| **Camera** | Make, Model, Lens Model |
| **Settings** | Aperture, Exposure Time, ISO, Focal Length, Flash |
| **Image** | Width, Height, Bit Depth, Color Space, Orientation |
| **GPS** | Latitude, Longitude, Altitude |
| **IPTC** | Caption, Keywords, Copyright, Photographer, Location |
| **Date** | Date Taken |

#### Configuration

| Setting | Description | Default |
|---------|-------------|---------|
| `extractGps` | Extract GPS coordinates | `true` |
| `extractThumbnails` | Generate thumbnails | `true` |
| `thumbnailSize` | Max thumbnail dimension (px) | `256` |
| `extractAllTags` | Extract all available tags | `false` |

---

### Video Metadata Extractor

**ID**: `com.vaultstadio.plugins.video-metadata`

Extracts metadata from video files using FFprobe.

> **Requirement**: FFprobe must be installed and available in PATH.

#### Supported Formats
- MP4, MKV, WebM, MOV, AVI
- FLV, WMV, MPEG, 3GP
- OGG

#### Extracted Metadata

| Category | Fields |
|----------|--------|
| **Video** | Width, Height, Codec, Frame Rate, Bitrate, Aspect Ratio |
| **Audio** | Codec, Sample Rate, Channels, Bitrate, Languages |
| **Container** | Format, Duration, Total Bitrate |
| **HDR** | Color Space, Color Transfer, HDR flag |
| **Content** | Title, Artist, Chapters, Subtitle Tracks |

#### Configuration

| Setting | Description | Default |
|---------|-------------|---------|
| `ffprobePath` | Path to FFprobe executable | `ffprobe` |
| `generateThumbnails` | Generate video thumbnails | `true` |
| `thumbnailPosition` | Position in seconds for thumbnail | `5` |
| `thumbnailSize` | Max thumbnail dimension (px) | `320` |
| `extractChapters` | Extract chapter information | `true` |
| `extractSubtitles` | List subtitle tracks | `true` |
| `timeout` | Command timeout in seconds | `30` |

---

### Full-Text Search

**ID**: `com.vaultstadio.plugins.fulltext-search`

Provides full-text search capabilities using Apache Lucene and content extraction via Apache Tika.

#### Supported Formats
- **Text**: Plain text, HTML, XML, CSS, JavaScript, Markdown, CSV
- **Documents**: PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, ODT/ODS/ODP
- **Code**: JSON, XML, YAML

#### Features
- Automatic indexing on file upload
- Content extraction from documents
- Relevance-scored search results
- Snippet generation with query highlighting
- Automatic de-indexing on file deletion

#### Configuration

| Setting | Description | Default |
|---------|-------------|---------|
| `autoIndex` | Automatically index on upload | `true` |
| `maxFileSize` | Maximum file size to index (MB) | `50` |
| `indexTextContent` | Extract and index text content | `true` |
| `maxResults` | Maximum search results | `100` |
| `snippetLength` | Length of text snippets | `200` |

#### Metadata Added

| Key | Description |
|-----|-------------|
| `wordCount` | Number of words in document |
| `indexed` | Whether file is indexed (`true`/`false`) |
| `indexedAt` | ISO timestamp of indexing |

---

## API Endpoints

### Get File Metadata

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
      "cameraModel": "EOS R5"
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

### Get Video Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata/video
Authorization: Bearer <token>
```

### Get Document Metadata

```http
GET /api/v1/storage/item/{itemId}/metadata/document
Authorization: Bearer <token>
```

### Search

```http
GET /api/v1/search?q=query&limit=50&offset=0
Authorization: Bearer <token>
```

### Advanced Search

```http
POST /api/v1/search/advanced
Authorization: Bearer <token>
Content-Type: application/json

{
  "query": "search terms",
  "searchContent": true,
  "fileTypes": ["pdf", "doc"],
  "minSize": 1024,
  "maxSize": 10485760,
  "limit": 50,
  "offset": 0
}
```

### Search Suggestions

```http
GET /api/v1/search/suggestions?prefix=doc&limit=10
Authorization: Bearer <token>
```

---

## Plugin Development

### Plugin Structure

```kotlin
class MyPlugin : AbstractPlugin() {
    override val metadata = PluginMetadata(
        id = "com.mycompany.my-plugin",
        name = "My Plugin",
        version = "1.0.0",
        description = "Description of my plugin",
        author = "Author Name",
        permissions = setOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA
        ),
        supportedMimeTypes = setOf("image/jpeg", "image/png")
    )
    
    override suspend fun onInitialize(context: PluginContext) {
        // Subscribe to events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            // Handle file upload
            EventHandlerResult.Success
        }
    }
    
    override suspend fun onShutdown() {
        // Cleanup
    }
}
```

### Available Hooks

| Hook | Description |
|------|-------------|
| `MetadataExtractionHook` | Extract metadata from files |
| `ThumbnailHook` | Generate thumbnails |
| `ContentAnalysisHook` | Analyze file content |

### Plugin Context APIs

| API | Description |
|-----|-------------|
| `context.storage` | Read file content |
| `context.metadata` | Save/retrieve metadata |
| `context.config` | Access plugin configuration |
| `context.eventBus` | Subscribe to events |
| `context.dataDirectory` | Plugin data storage path |
| `context.tempDirectory` | Temporary file storage |

### File Events

| Event | When Triggered |
|-------|----------------|
| `FileEvent.Uploaded` | After file upload completes |
| `FileEvent.Downloaded` | After file download |
| `FileEvent.Deleted` | After file deletion |
| `FileEvent.Moved` | After file move/rename |
| `FileEvent.Copied` | After file copy |
| `FileEvent.Starred` | After star/unstar |

---

## Metadata Keys

Standard metadata keys used across plugins:

```kotlin
object MetadataKeys {
    // Image
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val CAMERA_MAKE = "cameraMake"
    const val CAMERA_MODEL = "cameraModel"
    const val DATE_TAKEN = "dateTaken"
    const val APERTURE = "aperture"
    const val EXPOSURE_TIME = "exposureTime"
    const val ISO = "iso"
    const val FOCAL_LENGTH = "focalLength"
    const val GPS_LATITUDE = "gpsLatitude"
    const val GPS_LONGITUDE = "gpsLongitude"
    const val GPS_ALTITUDE = "gpsAltitude"
    const val COLOR_SPACE = "colorSpace"
    const val BIT_DEPTH = "bitDepth"
    
    // Video
    const val DURATION = "duration"
    const val VIDEO_CODEC = "videoCodec"
    const val AUDIO_CODEC = "audioCodec"
    const val FRAME_RATE = "frameRate"
    const val BITRATE = "bitrate"
    const val CHANNELS = "channels"
    const val SAMPLE_RATE = "sampleRate"
    
    // Common
    const val TITLE = "title"
    const val DESCRIPTION = "description"
}
```

---

## Dependencies

### Image Metadata Plugin
- `com.drewnoakes:metadata-extractor:2.19.0`

### Video Metadata Plugin
- FFprobe (external binary)
- FFmpeg (for thumbnail generation)

### Full-Text Search Plugin
- Apache Lucene 9.11.1
- Apache Tika 2.9.2

---

## Troubleshooting

### FFprobe Not Found

If video metadata extraction fails:

1. Install FFmpeg/FFprobe:
   - **macOS**: `brew install ffmpeg`
   - **Ubuntu**: `apt install ffmpeg`
   - **Docker**: Include FFmpeg in your Dockerfile

2. Configure the path:
   ```
   Plugin Config > Video Metadata > ffprobePath = /usr/bin/ffprobe
   ```

### Large File Indexing

If full-text indexing is slow or fails:

1. Increase `maxFileSize` limit
2. Disable `indexTextContent` for specific file types
3. Check available disk space for Lucene index

### Memory Usage

Plugins run in the same JVM as the main application. For large files:

1. Use streaming APIs where possible
2. Clean up temporary files
3. Configure JVM heap size appropriately
