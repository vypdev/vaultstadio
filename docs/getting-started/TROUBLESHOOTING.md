# VaultStadio Troubleshooting Guide

Common issues and their solutions for VaultStadio development and deployment.

## Build Issues

### Gradle Build Fails

**Problem**: `./gradlew build` fails with errors.

**Solutions**:

1. **Clean and rebuild**:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **Clear Gradle cache**:
   ```bash
   rm -rf ~/.gradle/caches/
   ./gradlew build --refresh-dependencies
   ```

3. **Check Java version**:
   ```bash
   java -version
   # Should be 17 or higher
   ```

4. **Update Gradle wrapper**:
   ```bash
   ./gradlew wrapper --gradle-version 8.5
   ```

### WASM Build Fails

**Problem**: WebAssembly build fails or produces errors.

**Solutions**:

1. **Ensure Kotlin 2.0+**:
   Check `gradle/libs.versions.toml` for Kotlin version.

2. **Clear browser cache**:
   WASM builds can be cached aggressively.

3. **Check WASM target**:
   ```kotlin
   // build.gradle.kts
   wasmJs {
       browser {
           commonWebpackConfig {
               outputFileName = "vaultstadio.js"
           }
       }
   }
   ```

### Cannot GET /files or /settings/change-password (Web)

**Problem**: Opening a direct URL (e.g. `http://localhost:8081/settings/change-password` or `http://localhost:8081/files`) returns "Cannot GET /path" instead of loading the app.

**Cause**: The dev server serves files by path; without SPA fallback it returns 404 for routes that exist only in the client-side router.

**Solutions**:

1. **Use the bundled dev server with SPA fallback** (if your setup supports it):  
   The project includes `compose-frontend/composeApp/webpack.config.d/03.devServer.js` which sets `historyApiFallback: true`. If you run `wasmJsBrowserRun` or `wasmJsBrowserDevelopmentRun` and the plugin merges this config, direct URLs should work.

2. **Serve the production build with an SPA-aware server**:
   ```bash
   ./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution
   npx serve -s compose-frontend/composeApp/build/dist/wasmJs/productionExecutable -l 8081
   ```
   Then open `http://localhost:8081/files` or `http://localhost:8081/settings/change-password`.

3. **Production (Docker/nginx)**: The image already uses an nginx config with `try_files $uri $uri/ /index.html;`, so direct URLs work in production.

### Desktop Build Issues

**Problem**: Desktop app won't compile or run.

**Solutions**:

1. **Check JVM target**:
   ```kotlin
   jvmToolchain(17)
   ```

2. **Missing fonts on Linux**:
   ```bash
   sudo apt install fonts-dejavu
   ```

3. **macOS security**:
   ```bash
   xattr -d com.apple.quarantine /path/to/app.app
   ```

## Frontend Issues

### "Unresolved reference" Errors

**Problem**: IDE shows unresolved references after changes.

**Solutions**:

1. **Sync Gradle**:
   Click "Sync Now" in IDE notification.

2. **Invalidate caches**:
   - IntelliJ: File → Invalidate Caches → Invalidate and Restart

3. **Clean and rebuild**:
   ```bash
   ./gradlew clean
   ./gradlew :compose-frontend:composeApp:build
   ```

### Navigation Not Working

**Problem**: Clicking sidebar items doesn't navigate.

**Solutions**:

1. **Check NavDestination enum**:
   Ensure destination exists in `NavDestination`.

2. **Verify App.kt when statement**:
   Each destination needs a case.

3. **Check currentDestination state**:
   ```kotlin
   var currentDestination by mutableStateOf(NavDestination.FILES)
   ```

### File Upload Fails

**Problem**: Files won't upload or upload fails.

**Solutions**:

1. **Check file size**:
   - Small files (<100MB): Standard upload
   - Large files (>100MB): Chunked upload

2. **Verify backend is running**:
   ```bash
   curl http://localhost:8080/api/v1/health
   ```

3. **Check CORS settings**:
   Backend must allow frontend origin.

4. **Browser storage quota**:
   Clear browser storage if quota exceeded.

### Drag & Drop Not Working

**Problem**: Dragging files doesn't trigger upload.

**Solutions**:

1. **Check platform support**:
   - Desktop: Full support
   - Web: Limited by browser

2. **Verify DragDrop initialization**:
   ```kotlin
   initializeDragDrop { files ->
       // Handle files
   }
   ```

3. **Check browser permissions**:
   Some browsers restrict file access.

## Backend Connection Issues

### "Network Error" Messages

**Problem**: Frontend can't connect to backend.

**Solutions**:

1. **Verify backend URL**:
   ```kotlin
   val viewModel = AppViewModel(baseUrl = "http://localhost:8080")
   ```

2. **Check CORS**:
   ```kotlin
   install(CORS) {
       allowHost("localhost:8080")
       allowHeader(HttpHeaders.ContentType)
       allowHeader(HttpHeaders.Authorization)
   }
   ```

3. **Firewall/proxy issues**:
   ```bash
   curl -v http://localhost:8080/api/v1/health
   ```

### Authentication Errors

**Problem**: Login fails or session expires.

**Solutions**:

1. **Check token storage**:
   - Desktop: Java Preferences
   - Web: localStorage

2. **Token expiration**:
   Default: 24 hours. Check `JWT_EXPIRATION`.

3. **Clear stored token**:
   ```kotlin
   PlatformStorage.remove(StorageKeys.AUTH_TOKEN)
   ```

### API Returns 500 Errors

**Problem**: Backend returns internal server errors.

**Solutions**:

1. **Check backend logs**:
   ```bash
   docker-compose logs -f backend
   ```

2. **Database connection**:
   ```bash
   docker-compose ps postgres
   ```

3. **Missing environment variables**:
   Check `.env` file or environment.

## Database Issues

### Database Connection Failed

**Problem**: Backend can't connect to PostgreSQL.

**Solutions**:

1. **Check PostgreSQL is running**:
   ```bash
   docker-compose ps
   ```

2. **Verify credentials**:
   ```bash
   DATABASE_URL=jdbc:postgresql://localhost:5432/vaultstadio
   DATABASE_USER=vaultstadio
   DATABASE_PASSWORD=vaultstadio
   ```

3. **Database doesn't exist**:
   ```bash
   docker exec -it postgres psql -U vaultstadio -c "\l"
   ```

### Migration Errors

**Problem**: Database schema migrations fail.

**Solutions**:

1. **Check Flyway migrations**:
   ```
   kotlin-backend/api/src/main/resources/db/migration/
   ```

2. **Reset database** (development only):
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

## Docker Issues

### Container Won't Start

**Problem**: Docker containers fail to start.

**Solutions**:

1. **Check Docker daemon**:
   ```bash
   docker info
   ```

2. **Port conflicts**:
   ```bash
   lsof -i :8080
   lsof -i :5432
   ```

3. **Rebuild containers**:
   ```bash
   docker-compose down
   docker-compose up --build
   ```

### Out of Disk Space

**Problem**: Docker runs out of space.

**Solutions**:

1. **Prune unused resources**:
   ```bash
   docker system prune -a
   ```

2. **Remove old images**:
   ```bash
   docker image prune
   ```

## Platform-Specific Issues

### iOS Build Fails

**Problem**: iOS app won't build in Xcode.

**Solutions**:

1. **Update Xcode Command Line Tools**:
   ```bash
   xcode-select --install
   ```

2. **Pod install**:
   ```bash
   cd compose-frontend/iosApp
   pod install
   ```

3. **Clean Xcode**:
   - Product → Clean Build Folder

### Android Build Fails

**Problem**: Android app won't compile.

**Solutions**:

1. **Update Android SDK**:
   Check `local.properties` for SDK path.

2. **Sync Gradle**:
   File → Sync Project with Gradle Files

3. **Clear Android caches**:
   File → Invalidate Caches

### Web Hot Reload Not Working

**Problem**: Changes don't reflect in browser.

**Solutions**:

1. **Hard refresh**:
   - Windows/Linux: Ctrl+Shift+R
   - macOS: Cmd+Shift+R

2. **Disable caching**:
   Open DevTools → Network → Disable cache

3. **Restart dev server**:
   ```bash
   ./gradlew :compose-frontend:composeApp:wasmJsBrowserDevelopmentRun
   ```

## Performance Issues

### Slow Initial Load

**Problem**: App takes long to load initially.

**Solutions**:

1. **Check network requests**:
   Use browser DevTools Network tab.

2. **Reduce initial data load**:
   Paginate large lists.

3. **Enable production build**:
   ```bash
   ./gradlew wasmJsBrowserProductionWebpack
   ```

### UI Lag/Jank

**Problem**: UI animations stutter.

**Solutions**:

1. **Use `remember`**:
   ```kotlin
   val filtered = remember(items) { items.filter { ... } }
   ```

2. **Use `LazyColumn`/`LazyGrid`**:
   ```kotlin
   LazyVerticalGrid(columns = GridCells.Adaptive(180.dp)) {
       items(items) { item -> ... }
   }
   ```

3. **Profile with Layout Inspector** (Android Studio).

## Getting Help

1. **Check logs**: Always check console/terminal output first.

2. **Search issues**: Check GitHub issues for similar problems.

3. **Create minimal reproduction**: Isolate the problem.

4. **Include details**:
   - Platform (Web/Desktop/Android/iOS)
   - OS version
   - Steps to reproduce
   - Error messages/stack traces
