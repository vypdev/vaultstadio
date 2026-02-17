# Metadata Extraction Guide

## Overview

VaultStadio can extract comprehensive metadata from various file types using built-in plugins:

- **Images**: EXIF, IPTC, XMP metadata (camera settings, GPS, dates, keywords)
- **Videos**: Duration, resolution, codecs, chapters (via FFprobe)
- **Documents**: Full-text content, word count, language detection (via Apache Tika)

## Plugin Architecture

Metadata extraction is handled by specialized plugins that are loaded automatically:

| Plugin ID | Description | Dependencies |
|-----------|-------------|--------------|
| `com.vaultstadio.plugins.image-metadata` | EXIF, IPTC, XMP extraction | metadata-extractor |
| `com.vaultstadio.plugins.video-metadata` | Video metadata via FFprobe | FFprobe (external) |
| `com.vaultstadio.plugins.fulltext-search` | Full-text indexing | Apache Lucene + Tika |

## Image Metadata Extraction

### Supported Formats

- JPEG, PNG, GIF, WebP, TIFF, BMP
- HEIC/HEIF (Apple photos)
- RAW formats: CR2 (Canon), NEF (Nikon), ARW (Sony)

### Extracted Metadata

#### EXIF Data
- **Camera Info**: Make, model, lens, software
- **Settings**: Aperture, shutter speed, ISO, focal length
- **Date/Time**: Date taken, creation date
- **Orientation**: Image orientation for correct display

#### GPS Data (configurable)
- Latitude and longitude coordinates
- Altitude
- Location name (if available)

#### IPTC Data
- Keywords and tags
- Caption/description
- Copyright information
- Photographer/author
- Location (city, state, country)

#### XMP Extended Metadata
- Rating and labels
- Color mode
- ICC profile

### Configuration

```yaml
# Plugin configuration
com.vaultstadio.plugins.image-metadata:
  extractGps: true          # Extract GPS coordinates
  extractThumbnails: true   # Generate thumbnail images
  thumbnailSize: 256        # Max dimension in pixels
  extractAllTags: false     # Extract all XMP tags (verbose)
  logUnknownTags: false     # Log unrecognized tags
```

## Video Metadata Extraction

### Requirements

FFprobe must be installed on the system:

```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt install ffmpeg

# Docker (included in Dockerfile)
```

### Supported Formats

- MP4, MKV, WebM, MOV, AVI
- FLV, WMV, MPEG
- 3GP, OGG

### Extracted Metadata

#### Format Information
- Duration (seconds and formatted)
- Container format (e.g., MPEG-4, Matroska)
- Overall bitrate
- Title, artist, comment (from tags)

#### Video Stream
- Resolution (width x height)
- Codec (e.g., h264, hevc, vp9)
- Frame rate
- Aspect ratio
- Pixel format
- HDR detection (color space, transfer)

#### Audio Stream
- Codec (e.g., aac, opus, mp3)
- Sample rate
- Channels and layout
- Language tracks

#### Additional
- Subtitle tracks
- Chapters (with timestamps)

### Configuration

```yaml
# Plugin configuration
com.vaultstadio.plugins.video-metadata:
  ffprobePath: "ffprobe"     # Path to FFprobe executable
  generateThumbnails: true   # Generate video thumbnails
  thumbnailPosition: 5       # Capture at N seconds
  thumbnailSize: 320         # Max dimension in pixels
  extractChapters: true      # Extract chapter info
  extractSubtitles: true     # List subtitle tracks
  timeout: 30                # Command timeout (seconds)
```

### Fallback Behavior

If FFprobe is not available, the plugin falls back to basic metadata:
- MIME type
- Container format (inferred from extension)
- A flag indicating limited extraction

## Document Indexing (Full-Text Search)

### Supported Formats

#### Text Files
- Plain text (.txt)
- Markdown (.md)
- HTML, CSS, JavaScript
- CSV, JSON, XML, YAML

#### Documents
- PDF
- Microsoft Word (.doc, .docx)
- Microsoft Excel (.xls, .xlsx)
- Microsoft PowerPoint (.ppt, .pptx)
- OpenDocument formats (.odt, .ods, .odp)

### Extracted Data

- Full text content (for search indexing)
- Word count
- Document title and author
- Creation date
- Language detection

### Configuration

```yaml
# Plugin configuration
com.vaultstadio.plugins.fulltext-search:
  autoIndex: true            # Index on upload
  maxFileSize: 50            # Max file size (MB)
  indexTextContent: true     # Extract and index content
  maxResults: 100            # Max search results
  snippetLength: 200         # Snippet length in results
```

## API Endpoints

### Get All Metadata

```http
GET /api/v1/storage/item/{id}/metadata
Authorization: Bearer <token>
```

Response:
```json
{
  "image": {
    "cameraMake": "Canon",
    "cameraModel": "EOS R5",
    "dateTaken": "2024-01-15T10:30:00Z",
    "aperture": "f/2.8",
    "iso": "400",
    "focalLength": "50mm",
    "gpsLatitude": "40.7128",
    "gpsLongitude": "-74.0060"
  }
}
```

### Get Image-Specific Metadata

```http
GET /api/v1/storage/item/{id}/metadata/image
Authorization: Bearer <token>
```

### Get Video-Specific Metadata

```http
GET /api/v1/storage/item/{id}/metadata/video
Authorization: Bearer <token>
```

Response:
```json
{
  "duration": 3600,
  "durationFormatted": "1:00:00",
  "width": 1920,
  "height": 1080,
  "videoCodec": "h264",
  "audioCodec": "aac",
  "frameRate": "23.98",
  "bitrate": 8000000,
  "chapters": [
    { "title": "Introduction", "time": "0:00" },
    { "title": "Main Content", "time": "5:30" }
  ]
}
```

### Get Document Metadata

```http
GET /api/v1/storage/item/{id}/metadata/document
Authorization: Bearer <token>
```

### Search by Metadata

```http
GET /api/v1/search/by-metadata?key=cameraModel&value=Canon
Authorization: Bearer <token>
```

### Full-Text Search

```http
GET /api/v1/search?q=meeting+notes
Authorization: Bearer <token>
```

Advanced search:
```http
POST /api/v1/search/advanced
Authorization: Bearer <token>
Content-Type: application/json

{
  "query": "quarterly report",
  "mimeTypes": ["application/pdf"],
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

## Metadata Keys Reference

### Image Metadata Keys

| Key | Description | Example |
|-----|-------------|---------|
| `cameraMake` | Camera manufacturer | "Canon" |
| `cameraModel` | Camera model | "EOS R5" |
| `dateTaken` | Original capture date | "2024-01-15T10:30:00Z" |
| `aperture` | Aperture value | "f/2.8" |
| `exposureTime` | Shutter speed | "1/250" |
| `iso` | ISO sensitivity | "400" |
| `focalLength` | Focal length | "50mm" |
| `width` | Image width (pixels) | "6000" |
| `height` | Image height (pixels) | "4000" |
| `gpsLatitude` | GPS latitude | "40.7128" |
| `gpsLongitude` | GPS longitude | "-74.0060" |
| `gpsAltitude` | GPS altitude (meters) | "10.5" |
| `colorSpace` | Color space | "sRGB" |
| `bitDepth` | Bits per sample | "8" |
| `description` | Caption/description | "..." |

### Video Metadata Keys

| Key | Description | Example |
|-----|-------------|---------|
| `duration` | Duration (seconds) | "3600" |
| `durationFormatted` | Human-readable duration | "1:00:00" |
| `width` | Video width | "1920" |
| `height` | Video height | "1080" |
| `videoCodec` | Video codec | "h264" |
| `audioCodec` | Audio codec | "aac" |
| `frameRate` | Frame rate (fps) | "23.98" |
| `bitrate` | Overall bitrate | "8000000" |
| `sampleRate` | Audio sample rate | "48000" |
| `channels` | Audio channels | "2" |
| `isHDR` | HDR content flag | "true" |

### Document Metadata Keys

| Key | Description | Example |
|-----|-------------|---------|
| `wordCount` | Number of words | "1500" |
| `title` | Document title | "Annual Report" |
| `author` | Document author | "John Doe" |
| `indexed` | Search indexed flag | "true" |
| `indexedAt` | Indexing timestamp | "2024-01-15T10:30:00Z" |

## Automatic Processing

Metadata extraction happens automatically when files are uploaded:

1. **File Upload**: User uploads a file via UI or API
2. **Event Triggered**: `FileEvent.Uploaded` event is published
3. **Plugin Processing**: Relevant plugins receive the event
4. **Metadata Extraction**: Plugins extract metadata based on MIME type
5. **Storage**: Metadata is saved to the database
6. **Indexing**: Full-text content is indexed for search (if applicable)

## Troubleshooting

### No Metadata Extracted

1. **Check plugin status**: Verify plugins are enabled
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/plugins
   ```

2. **Check file type support**: Ensure MIME type is supported

3. **Review logs**: Check backend logs for extraction errors

### Video Metadata Missing

1. **Verify FFprobe installation**:
   ```bash
   ffprobe -version
   ```

2. **Check plugin logs**: Look for "FFprobe not found" warnings

3. **Verify timeout**: Large files may exceed the default timeout

### Search Not Working

1. **Verify indexing**:
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/storage/item/{id}/metadata
   ```
   Look for `indexed: true`

2. **Rebuild index** (admin):
   ```bash
   curl -X POST -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/plugins/fulltext-search/rebuild-index
   ```

### GPS Not Extracted

GPS extraction can be disabled for privacy. Check configuration:
```yaml
com.vaultstadio.plugins.image-metadata:
  extractGps: true  # Must be true
```

## Performance Considerations

- **Large files**: Consider increasing timeout for video metadata
- **Batch uploads**: Metadata extraction is async and won't block uploads
- **Storage**: Extracted metadata adds ~1-10KB per file
- **Search index**: Full-text index grows with document content

## Security Notes

- GPS data may contain sensitive location information
- Consider disabling GPS extraction for privacy-sensitive deployments
- Full-text search indexes document content - ensure proper access controls
