# VaultStadio Plugin Development Guide

## Overview

VaultStadio plugins extend the platform's functionality without modifying the core. Plugins can:

- React to storage events (uploads, downloads, deletions)
- Extract and attach metadata to files
- Register custom API endpoints
- Run background tasks
- Integrate with external services

## Getting Started

### 1. Create a new Gradle project

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
    implementation("com.vaultstadio:plugins-api:1.0.0")
}
```

### 2. Implement the Plugin interface

```kotlin
package com.example.myplugin

import com.vaultstadio.plugins.api.*
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.core.domain.event.*

class MyPlugin : AbstractPlugin() {
    
    override val metadata = PluginMetadata(
        id = "com.example.myplugin",
        name = "My Plugin",
        version = "1.0.0",
        description = "Description of what this plugin does",
        author = "Your Name",
        website = "https://example.com",
        permissions = listOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA
        )
    )
    
    override suspend fun onInitialize(context: PluginContext) {
        super.onInitialize(context)
        
        // Subscribe to events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            context.logger.info { "File uploaded: ${event.item.name}" }
            
            // Do something with the file
            processFile(event.item, context)
            
            EventHandlerResult.Success
        }
        
        // Register custom endpoint
        context.registerEndpoint("GET", "/status") { request ->
            EndpointResponse.ok("""{"status": "running"}""")
        }
    }
    
    private suspend fun processFile(item: StorageItem, context: PluginContext) {
        // Process the file and save metadata
        context.metadata.setValue(item.id, "processed", "true")
    }
    
    override suspend fun onShutdown() {
        // Cleanup resources
    }
}
```

### 3. Register with ServiceLoader

Create file: `src/main/resources/META-INF/services/com.vaultstadio.plugins.api.Plugin`

```
com.example.myplugin.MyPlugin
```

### 4. Build and install

```bash
./gradlew build

# Copy JAR to plugins directory
cp build/libs/myplugin-1.0.0.jar /data/plugins/
```

## Plugin API Reference

### PluginContext

The context provides access to VaultStadio APIs:

```kotlin
interface PluginContext {
    val pluginId: String
    val scope: CoroutineScope      // For async operations
    val eventBus: EventBus         // Subscribe to/publish events
    val storage: StorageApi        // Read files
    val metadata: MetadataApi      // Read/write metadata
    val users: UserApi             // Get user info
    val logger: PluginLogger       // Logging
    val config: ConfigStore        // Plugin configuration
    val tempDirectory: Path        // Temp storage
    val dataDirectory: Path        // Persistent storage
    val httpClient: HttpClientApi? // External HTTP requests
    
    fun registerEndpoint(...)      // Register API endpoints
    suspend fun scheduleTask(...)  // Schedule background tasks
}
```

### Events

Subscribe to events to react to storage operations:

```kotlin
// File events
FileEvent.Uploaded      // File was uploaded
FileEvent.Downloaded    // File was downloaded
FileEvent.Deleting      // File is about to be deleted (can abort)
FileEvent.Deleted       // File was deleted
FileEvent.Moved         // File was moved
FileEvent.Renamed       // File was renamed
FileEvent.Copied        // File was copied
FileEvent.Restored      // File was restored from trash
FileEvent.StarredChanged // Star status changed

// Folder events
FolderEvent.Created     // Folder was created
FolderEvent.Deleted     // Folder was deleted
FolderEvent.Moved       // Folder was moved

// Share events
ShareEvent.Created      // Share link created
ShareEvent.Accessed     // Share was accessed
ShareEvent.Deleted      // Share was deleted

// User events
UserEvent.LoggedIn      // User logged in
UserEvent.LoggedOut     // User logged out
UserEvent.Created       // User account created
```

### Metadata API

Attach custom metadata to files:

```kotlin
// Set a single value
context.metadata.setValue(itemId, "key", "value")

// Set multiple values
context.metadata.setValues(itemId, mapOf(
    "width" to "1920",
    "height" to "1080",
    "codec" to "h264"
))

// Get values
val width = context.metadata.getValue(itemId, "width")
val allMetadata = context.metadata.getMetadata(itemId)

// Search by metadata
val itemIds = context.metadata.searchByValue("codec", "h264")
```

### Configuration Schema

Define configuration options for your plugin:

```kotlin
override fun getConfigurationSchema() = pluginConfiguration {
    string("apiKey", "API Key") {
        description = "Your API key for the external service"
        required = true
    }
    
    boolean("autoProcess", "Auto-process uploads") {
        defaultValue = "true"
    }
    
    select("quality", "Processing Quality", listOf(
        ConfigOption("low", "Low (fast)"),
        ConfigOption("medium", "Medium"),
        ConfigOption("high", "High (slow)")
    )) {
        defaultValue = "medium"
    }
    
    number("maxFileSize", "Max File Size (MB)") {
        defaultValue = "100"
        validate(minValue = 1.0, maxValue = 10000.0)
    }
}
```

### Custom Endpoints

Register API endpoints accessible at `/api/v1/plugins/{pluginId}/`:

```kotlin
// GET /api/v1/plugins/com.example.myplugin/status
context.registerEndpoint("GET", "/status") { request ->
    val userId = request.userId // Authenticated user ID
    EndpointResponse.ok("""{"status": "ok"}""")
}

// POST /api/v1/plugins/com.example.myplugin/process
context.registerEndpoint("POST", "/process") { request ->
    val body = request.body ?: return EndpointResponse.badRequest("No body")
    // Process request...
    EndpointResponse.created("""{"id": "123"}""")
}
```

### Background Tasks

Schedule periodic or one-time tasks:

```kotlin
// Run once
val taskId = context.scheduleTask("cleanup", null) {
    // Do cleanup
}

// Run periodically (cron expression)
val taskId = context.scheduleTask("sync", "0 */5 * * * *") {
    // Run every 5 minutes
}

// Cancel task
context.cancelTask(taskId)
```

## Hooks

Hooks allow intercepting core operations:

### Pre-Upload Hook

Validate or modify files before upload:

```kotlin
class MyPlugin : AbstractPlugin(), PreUploadHook {
    
    override suspend fun beforeUpload(
        fileName: String,
        mimeType: String?,
        size: Long,
        context: HookContext
    ): HookResult<PreUploadData> {
        // Block certain file types
        if (mimeType?.contains("executable") == true) {
            return HookResult.Abort("Executable files not allowed")
        }
        
        // Continue with upload
        return HookResult.Continue(PreUploadData(fileName, mimeType))
    }
}
```

### Metadata Extraction Hook

Extract metadata automatically:

```kotlin
class ImageMetadataPlugin : AbstractPlugin(), MetadataExtractionHook {
    
    override fun supportedMimeTypes() = listOf("image/*")
    
    override suspend fun extractMetadata(
        item: StorageItem,
        inputStream: InputStream
    ): Map<String, String> {
        // Extract and return metadata
        return mapOf(
            "width" to "1920",
            "height" to "1080"
        )
    }
}
```

## Best Practices

1. **Handle errors gracefully**: Log errors and return appropriate results
2. **Use coroutines**: All plugin operations should be non-blocking
3. **Respect permissions**: Only request permissions you need
4. **Clean up resources**: Implement `onShutdown` to clean up
5. **Use configuration**: Make behavior configurable
6. **Version properly**: Follow semantic versioning
7. **Document your plugin**: Include clear README and usage instructions

## Example Plugins

### Image Metadata Extractor

```kotlin
class ImageMetadataPlugin : MetadataExtractorPlugin() {
    
    override val metadata = PluginMetadata(
        id = "com.vaultstadio.image-metadata",
        name = "Image Metadata",
        version = "1.0.0",
        description = "Extracts EXIF and image metadata",
        author = "VaultStadio",
        permissions = listOf(READ_FILES, WRITE_METADATA)
    )
    
    override val extractorConfig = MetadataExtractorConfig(
        autoExtract = true,
        generateThumbnails = true
    )
    
    override fun supportedMimeTypes() = listOf("image/*")
    
    override suspend fun extract(
        item: StorageItem,
        inputStream: InputStream
    ): ExtractedMetadata {
        val metadata = mutableMapOf<String, String>()
        
        // Read image and extract metadata
        // ... implementation
        
        return ExtractedMetadata(
            values = metadata,
            thumbnail = thumbnailBytes
        )
    }
}
```

### Virus Scanner

```kotlin
class VirusScannerPlugin : AbstractPlugin(), PreUploadHook {
    
    override val metadata = PluginMetadata(
        id = "com.example.virus-scanner",
        name = "Virus Scanner",
        version = "1.0.0",
        description = "Scans uploads for viruses",
        author = "Example",
        permissions = listOf(READ_FILES, NETWORK)
    )
    
    override suspend fun beforeUpload(
        fileName: String,
        mimeType: String?,
        size: Long,
        context: HookContext
    ): HookResult<PreUploadData> {
        // Scan file...
        val isClean = scanFile(...)
        
        return if (isClean) {
            HookResult.Continue(PreUploadData(fileName, mimeType))
        } else {
            HookResult.Abort("File contains malware")
        }
    }
}
```
