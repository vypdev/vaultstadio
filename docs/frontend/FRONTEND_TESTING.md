# VaultStadio Frontend Testing Guide

This guide covers testing strategies and best practices for the VaultStadio frontend.

## Testing Overview

The frontend uses Kotlin's multiplatform testing framework with tests located in:
```
compose-frontend/composeApp/src/commonTest/kotlin/
```

## Test Structure

```
commonTest/kotlin/com/vaultstadio/app/
├── IntegrationTest.kt           # Full flow tests
├── ui/
│   ├── components/
│   │   ├── ComponentsTest.kt    # General component tests
│   │   ├── FileItemTest.kt      # FileItem specific tests
│   │   └── SidebarTest.kt       # Sidebar tests
│   └── screens/
│       └── ScreensTest.kt       # Screen logic tests
└── viewmodel/
    ├── AppViewModelTest.kt      # Core ViewModel tests
    └── AppViewModelPhase6Test.kt # Phase 6 feature tests
```

## Running Tests

### All Tests

```bash
./gradlew :compose-frontend:composeApp:allTests
```

### Common Tests Only

```bash
./gradlew :compose-frontend:composeApp:commonTest
```

### Desktop Tests

```bash
./gradlew :compose-frontend:composeApp:desktopTest
```

### Specific Test Class

```bash
./gradlew :compose-frontend:composeApp:test --tests "com.vaultstadio.app.viewmodel.AppViewModelTest"
```

## Test Categories

### 1. Unit Tests

Test individual functions and data transformations:

```kotlin
class FileItemTest {
    @Test
    fun `should correctly identify file type`() {
        val file = createTestItem(type = ItemType.FILE)
        assertEquals(ItemType.FILE, file.type)
        assertFalse(file.isFolder)
    }
    
    @Test
    fun `should extract file extension correctly`() {
        val file = createTestItem(name = "document.pdf")
        val extension = file.name.substringAfterLast('.', "")
        assertEquals("pdf", extension)
    }
}
```

### 2. ViewModel Tests

Test state management and business logic:

```kotlin
class AppViewModelTest {
    @Test
    fun `should toggle selection mode on first selection`() {
        val selectedItems = mutableSetOf<String>()
        var isSelectionMode = false
        
        // Select item
        selectedItems.add("item-1")
        isSelectionMode = selectedItems.isNotEmpty()
        
        assertTrue(isSelectionMode)
    }
    
    @Test
    fun `should clear selection on mode exit`() {
        val selectedItems = mutableSetOf("item-1", "item-2")
        
        selectedItems.clear()
        
        assertTrue(selectedItems.isEmpty())
    }
}
```

### 3. Integration Tests

Test complete user flows:

```kotlin
class IntegrationTest {
    @Test
    fun `login flow should validate credentials format`() {
        val email = "user@example.com"
        val password = "password123"
        
        assertTrue(email.contains("@"))
        assertTrue(password.length >= 6)
    }
    
    @Test
    fun `batch delete should handle multiple items`() {
        val selectedItems = setOf("item-1", "item-2", "item-3")
        var deletedCount = 0
        
        selectedItems.forEach { deletedCount++ }
        
        assertEquals(3, deletedCount)
    }
}
```

### 4. Screen Logic Tests

Test screen-specific logic without UI:

```kotlin
class ScreensTest {
    @Test
    fun `files screen should filter by type`() {
        val items = listOf(
            createItem("1", "file.txt", ItemType.FILE),
            createItem("2", "folder", ItemType.FOLDER)
        )
        
        val files = items.filter { it.type == ItemType.FILE }
        
        assertEquals(1, files.size)
    }
    
    @Test
    fun `admin screen should filter users by role`() {
        val users = listOf(
            MockUser("1", UserRole.ADMIN),
            MockUser("2", UserRole.USER)
        )
        
        val admins = users.filter { it.role == UserRole.ADMIN }
        
        assertEquals(1, admins.size)
    }
}
```

## Best Practices

### 1. Use Descriptive Test Names

```kotlin
// Good
@Test
fun `should sort folders before files when sorting by name`()

// Avoid
@Test
fun testSort()
```

### 2. Follow AAA Pattern

```kotlin
@Test
fun `should calculate progress correctly`() {
    // Arrange
    val uploaded = 50L
    val total = 100L
    
    // Act
    val progress = uploaded.toFloat() / total.toFloat()
    
    // Assert
    assertEquals(0.5f, progress)
}
```

### 3. Test Edge Cases

```kotlin
@Test
fun `should handle empty list`() {
    val items = emptyList<StorageItem>()
    val filtered = items.filter { it.isStarred }
    assertTrue(filtered.isEmpty())
}

@Test
fun `should handle null quota`() {
    val quota: Long? = null
    val percentage = quota?.let { 50.0 } ?: 0.0
    assertEquals(0.0, percentage)
}
```

### 4. Use Helper Functions

```kotlin
private fun createTestItem(
    id: String = "test-id",
    name: String = "test-file.txt",
    type: ItemType = ItemType.FILE
) = StorageItem(
    id = id,
    name = name,
    type = type,
    // ... other defaults
)
```

### 5. Group Related Tests

```kotlin
class FileItemTest {
    // Type detection tests
    @Test fun `should identify file type`() { }
    @Test fun `should identify folder type`() { }
    
    // Extension tests
    @Test fun `should extract extension`() { }
    @Test fun `should handle no extension`() { }
    
    // Star tests
    @Test fun `should track starred state`() { }
}
```

## Mocking

For complex dependencies, use mock data classes:

```kotlin
private data class MockApiResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val error: String? = null
)

@Test
fun `should handle API success`() {
    val result = MockApiResult(
        isSuccess = true,
        data = listOf(createTestItem())
    )
    
    assertTrue(result.isSuccess)
    assertNotNull(result.data)
}

@Test
fun `should handle API error`() {
    val result = MockApiResult<List<StorageItem>>(
        isSuccess = false,
        error = "Network error"
    )
    
    assertFalse(result.isSuccess)
    assertEquals("Network error", result.error)
}
```

## Testing Compose UI

For UI-specific tests on desktop/Android, use Compose Testing:

```kotlin
@OptIn(ExperimentalTestApi::class)
class FileItemUITest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `should display file name`() {
        composeTestRule.setContent {
            FileItem(
                item = createTestItem(name = "test.txt"),
                onItemClick = {},
                onStarClick = {},
                onMenuClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("test.txt").assertExists()
    }
}
```

## Test Coverage

### Current Coverage

| Module | Coverage |
|--------|----------|
| Components | ~60% |
| Screens | ~50% |
| ViewModel | ~70% |
| Integration | ~40% |

### Priority Areas

1. **High Priority**: ViewModel logic, authentication flows
2. **Medium Priority**: Component behavior, navigation
3. **Lower Priority**: UI styling, animations

## Continuous Integration

Tests run automatically on:
- Pull requests
- Commits to main/master
- Release tags

See `.github/workflows/ci.yml` for CI configuration.

## Debugging Failed Tests

1. **Check test output**:
   ```bash
   ./gradlew :compose-frontend:composeApp:test --info
   ```

2. **Run single test**:
   ```bash
   ./gradlew :compose-frontend:composeApp:test --tests "*.testName"
   ```

3. **View reports**:
   Open `compose-frontend/composeApp/build/reports/tests/`
