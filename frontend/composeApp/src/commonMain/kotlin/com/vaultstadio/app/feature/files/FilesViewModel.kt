package com.vaultstadio.app.feature.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.Activity
import com.vaultstadio.app.domain.model.Breadcrumb
import com.vaultstadio.app.domain.model.ShareLink
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.ViewMode
import com.vaultstadio.app.domain.usecase.activity.GetItemActivityUseCase
import com.vaultstadio.app.domain.usecase.config.GetShareUrlUseCase
import com.vaultstadio.app.domain.usecase.config.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.usecase.metadata.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.usecase.share.CreateShareUseCase
import com.vaultstadio.app.domain.usecase.storage.BatchCopyUseCase
import com.vaultstadio.app.domain.usecase.storage.BatchDeleteUseCase
import com.vaultstadio.app.domain.usecase.storage.BatchMoveUseCase
import com.vaultstadio.app.domain.usecase.storage.BatchStarUseCase
import com.vaultstadio.app.domain.usecase.storage.CreateFolderUseCase
import com.vaultstadio.app.domain.usecase.storage.DeleteItemUseCase
import com.vaultstadio.app.domain.usecase.storage.EmptyTrashUseCase
import com.vaultstadio.app.domain.usecase.storage.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.usecase.storage.GetFolderItemsUseCase
import com.vaultstadio.app.domain.usecase.storage.GetRecentUseCase
import com.vaultstadio.app.domain.usecase.storage.GetStarredUseCase
import com.vaultstadio.app.domain.usecase.storage.GetTrashUseCase
import com.vaultstadio.app.domain.usecase.storage.RenameItemUseCase
import com.vaultstadio.app.domain.usecase.storage.RestoreItemUseCase
import com.vaultstadio.app.domain.usecase.storage.SearchUseCase
import com.vaultstadio.app.domain.usecase.storage.ToggleStarUseCase
import com.vaultstadio.app.domain.usecase.storage.TrashItemUseCase
import com.vaultstadio.app.feature.main.MainComponent
import com.vaultstadio.app.platform.PlatformStorage
import com.vaultstadio.app.platform.StorageKeys
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/**
 * ViewModel for file management screens (Files, Recent, Starred, Trash).
 * Loading and path resolution are delegated to [FilesLoader]; this class holds state and UI callbacks.
 */
@KoinViewModel
class FilesViewModel(
    private val getFolderItemsUseCase: GetFolderItemsUseCase,
    private val getRecentUseCase: GetRecentUseCase,
    private val getStarredUseCase: GetStarredUseCase,
    private val getTrashUseCase: GetTrashUseCase,
    private val getBreadcrumbsUseCase: GetBreadcrumbsUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val renameItemUseCase: RenameItemUseCase,
    private val toggleStarUseCase: ToggleStarUseCase,
    private val trashItemUseCase: TrashItemUseCase,
    private val restoreItemUseCase: RestoreItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val emptyTrashUseCase: EmptyTrashUseCase,
    private val batchDeleteUseCase: BatchDeleteUseCase,
    private val batchMoveUseCase: BatchMoveUseCase,
    private val batchCopyUseCase: BatchCopyUseCase,
    private val batchStarUseCase: BatchStarUseCase,
    private val searchUseCase: SearchUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getItemActivityUseCase: GetItemActivityUseCase,
    private val createShareUseCase: CreateShareUseCase,
    private val getStorageUrlsUseCase: GetStorageUrlsUseCase,
    private val getShareUrlUseCase: GetShareUrlUseCase,
    @InjectedParam private val mode: MainComponent.FilesMode,
) : ViewModel() {

    companion object {
        private const val FOLDER_PAGE_SIZE = 50
    }

    private val loader = FilesLoader(
        getFolderItemsUseCase = getFolderItemsUseCase,
        getRecentUseCase = getRecentUseCase,
        getStarredUseCase = getStarredUseCase,
        getTrashUseCase = getTrashUseCase,
        getBreadcrumbsUseCase = getBreadcrumbsUseCase,
        folderPageSize = FOLDER_PAGE_SIZE,
    )

    // Navigation state
    var currentFolderId by mutableStateOf<String?>(null)
        private set
    var currentFolderName by mutableStateOf<String?>(null)
        private set
    var breadcrumbs by mutableStateOf<List<Breadcrumb>>(emptyList())
        private set

    // Content state
    var items by mutableStateOf<List<StorageItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // View state
    var viewMode by mutableStateOf(ViewMode.GRID)
        private set
    var sortField by mutableStateOf(SortField.TYPE)
        private set
    var sortOrder by mutableStateOf(SortOrder.ASC)
        private set

    // Pagination
    var folderTotalItems by mutableStateOf(0L)
        private set
    var folderHasMore by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set

    // Multi-selection
    var selectedItems by mutableStateOf<Set<String>>(emptySet())
        private set
    val isSelectionMode: Boolean get() = selectedItems.isNotEmpty()

    // Info panel
    var showInfoPanel by mutableStateOf(false)
        private set
    var selectedInfoItem by mutableStateOf<StorageItem?>(null)
        private set
    var itemActivity by mutableStateOf<List<Activity>>(emptyList())
        private set
    var isLoadingActivity by mutableStateOf(false)
        private set

    private var displayedMode: MainComponent.FilesMode = mode

    // Search
    var searchQuery by mutableStateOf("")
        private set
    var isSearching by mutableStateOf(false)
        private set
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())
        private set

    // Share
    var isCreatingShare by mutableStateOf(false)
        private set
    var lastCreatedShare by mutableStateOf<ShareLink?>(null)
        private set
    var shareError by mutableStateOf<String?>(null)
        private set

    init {
        restoreFilesViewPreferences()
        loadItems()
    }

    private fun restoreFilesViewPreferences() {
        PlatformStorage.getString(StorageKeys.VIEW_MODE)?.let { value ->
            enumValues<ViewMode>().find { it.name == value }?.let { viewMode = it }
        }
        PlatformStorage.getString(StorageKeys.FILES_SORT_FIELD)?.let { value ->
            enumValues<SortField>().find { it.name == value }?.let { sortField = it }
        }
        PlatformStorage.getString(StorageKeys.FILES_SORT_ORDER)?.let { value ->
            enumValues<SortOrder>().find { it.name == value }?.let { sortOrder = it }
        }
    }

    // --- Navigation ---
    fun navigateToFolder(folderId: String?, folderName: String? = null) {
        currentFolderId = folderId
        currentFolderName = folderName?.takeIf { it.isNotBlank() }
        clearSelection()
        if (mode == MainComponent.FilesMode.ALL) {
            breadcrumbs = if (folderId == null) {
                listOf(Breadcrumb(id = null, name = "Home", path = "/"))
            } else {
                listOf(
                    Breadcrumb(id = null, name = "Home", path = "/"),
                    Breadcrumb(id = folderId, name = folderName ?: "", path = "/"),
                )
            }
        }
        loadItems()
    }

    fun openPathFromSegments(segments: List<String>) {
        if (mode != MainComponent.FilesMode.ALL || segments.isEmpty()) return
        viewModelScope.launch {
            val resolved = loader.resolvePathSegments(segments, sortField, sortOrder)
            resolved?.let { (id, name) -> navigateToFolder(id, name) }
        }
    }

    fun navigateUp() {
        val parentIndex = breadcrumbs.size - 2
        if (parentIndex >= 0) {
            val parent = breadcrumbs[parentIndex]
            navigateToFolder(parent.id, parent.name)
        } else {
            navigateToFolder(null)
        }
    }

    fun refresh() = loadItems()

    fun loadItemsForMode(displayedMode: MainComponent.FilesMode) {
        this.displayedMode = displayedMode
        viewModelScope.launch {
            isLoading = true
            error = null
            when (
                val result = loader.loadItemsForMode(
                    mode = displayedMode,
                    currentFolderId = currentFolderId,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    currentFolderName = currentFolderName,
                )
            ) {
                is LoadItemsResult.Success -> {
                    items = result.items
                    folderTotalItems = result.total
                    folderHasMore = result.hasMore
                    breadcrumbs = result.breadcrumbs
                    if (currentFolderId == null && displayedMode == MainComponent.FilesMode.ALL) {
                        currentFolderName = null
                    } else {
                        currentFolderName = result.currentFolderName
                    }
                }
                is LoadItemsResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    private fun loadItems() = loadItemsForMode(displayedMode)

    fun loadMoreFolderItems() {
        if (displayedMode != MainComponent.FilesMode.ALL || !folderHasMore || isLoading || isLoadingMore) return
        viewModelScope.launch {
            isLoadingMore = true
            when (
                val result = loader.loadMoreFolderItems(
                    currentFolderId = currentFolderId,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    currentOffset = items.size,
                )
            ) {
                is LoadMoreResult.Success -> {
                    items = items + result.newItems
                    folderHasMore = result.hasMore
                }
                is LoadMoreResult.Error -> error = result.message
            }
            isLoadingMore = false
        }
    }

    private fun runOp(
        block: suspend () -> ApiResult<*>,
        onSuccess: () -> Unit = { loadItems() },
    ) {
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    private fun runOpWithLoading(
        block: suspend () -> ApiResult<*>,
        onSuccess: () -> Unit = { loadItems() },
    ) {
        viewModelScope.launch {
            isLoading = true
            when (val result = block()) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    // --- File operations ---
    fun createFolder(name: String, onSuccess: () -> Unit = {}) {
        runOpWithLoading(
            block = { createFolderUseCase(name, currentFolderId) },
            onSuccess = {
                loadItems()
                onSuccess()
            },
        )
    }

    fun renameItem(itemId: String, newName: String) {
        runOp(block = { renameItemUseCase(itemId, newName) })
    }

    fun toggleStar(item: StorageItem) {
        viewModelScope.launch {
            when (val result = toggleStarUseCase(item.id)) {
                is ApiResult.Success -> {
                    loadItems()
                    if (selectedInfoItem?.id == item.id) {
                        selectedInfoItem = items.find { it.id == item.id }
                            ?: selectedInfoItem?.copy(isStarred = !item.isStarred)
                    }
                }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun trashItem(itemId: String) = runOp(block = { trashItemUseCase(itemId) })
    fun restoreItem(itemId: String) = runOp(block = { restoreItemUseCase(itemId) })
    fun deleteItemPermanently(itemId: String) = runOp(block = { deleteItemUseCase(itemId) })

    fun emptyTrash(displayedMode: MainComponent.FilesMode) {
        runOpWithLoading(
            block = { emptyTrashUseCase() },
            onSuccess = { loadItemsForMode(displayedMode) },
        )
    }

    // --- Selection ---
    fun toggleItemSelection(itemId: String) {
        selectedItems = if (itemId in selectedItems) selectedItems - itemId else selectedItems + itemId
    }

    fun selectAll() {
        val allIds = items.map { it.id }.toSet()
        selectedItems = if (selectedItems == allIds) emptySet() else allIds
    }

    fun clearSelection() {
        selectedItems = emptySet()
    }

    // --- Batch ---
    fun batchDelete(permanent: Boolean = false) {
        if (selectedItems.isEmpty()) return
        runOpWithLoading(
            block = { batchDeleteUseCase(selectedItems.toList(), permanent) },
            onSuccess = {
                clearSelection()
                loadItems()
            },
        )
    }

    fun batchMove(destinationId: String?) {
        if (selectedItems.isEmpty()) return
        runOpWithLoading(
            block = { batchMoveUseCase(selectedItems.toList(), destinationId) },
            onSuccess = {
                clearSelection()
                loadItems()
            },
        )
    }

    fun moveItemToFolder(itemId: String, destinationId: String?) {
        viewModelScope.launch {
            items = items.filter { it.id != itemId }
            when (val result = batchMoveUseCase(listOf(itemId), destinationId)) {
                is ApiResult.Success -> loadItems()
                is ApiResult.Error -> {
                    error = result.message
                    loadItems()
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    loadItems()
                }
            }
        }
    }

    fun batchCopy(destinationId: String?) {
        if (selectedItems.isEmpty()) return
        runOpWithLoading(
            block = { batchCopyUseCase(selectedItems.toList(), destinationId) },
            onSuccess = {
                clearSelection()
                loadItems()
            },
        )
    }

    fun batchStar(starred: Boolean) {
        if (selectedItems.isEmpty()) return
        runOp(
            block = { batchStarUseCase(selectedItems.toList(), starred) },
            onSuccess = {
                clearSelection()
                loadItems()
            },
        )
    }

    // --- Info panel ---
    fun showItemInfo(item: StorageItem) {
        selectedInfoItem = item
        showInfoPanel = true
        loadItemActivity(item.id)
    }

    fun hideItemInfo() {
        showInfoPanel = false
        selectedInfoItem = null
        itemActivity = emptyList()
    }

    fun loadItemActivity(itemId: String, limit: Int = 10) {
        viewModelScope.launch {
            isLoadingActivity = true
            when (val result = getItemActivityUseCase(itemId, limit)) {
                is ApiResult.Success -> itemActivity = result.data
                else -> { }
            }
            isLoadingActivity = false
        }
    }

    // --- View controls ---
    fun updateViewMode(mode: ViewMode) {
        viewMode = mode
        PlatformStorage.setString(StorageKeys.VIEW_MODE, mode.name)
    }

    fun updateSortField(field: SortField) {
        if (sortField != field) {
            sortField = field
            PlatformStorage.setString(StorageKeys.FILES_SORT_FIELD, field.name)
            loadItems()
        }
    }

    fun updateSortOrder(order: SortOrder) {
        if (sortOrder != order) {
            sortOrder = order
            PlatformStorage.setString(StorageKeys.FILES_SORT_ORDER, order.name)
            loadItems()
        }
    }

    // --- Search ---
    fun search(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            loadItems()
            return
        }
        viewModelScope.launch {
            isSearching = true
            when (val result = searchUseCase(query)) {
                is ApiResult.Success -> items = result.data.items
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isSearching = false
        }
    }

    fun clearSearch() {
        searchQuery = ""
        searchSuggestions = emptyList()
        loadItems()
    }

    fun loadSearchSuggestions(prefix: String) {
        if (prefix.length < 2) {
            searchSuggestions = emptyList()
            return
        }
        viewModelScope.launch {
            when (val result = getSearchSuggestionsUseCase(prefix)) {
                is ApiResult.Success -> searchSuggestions = result.data
                else -> { }
            }
        }
    }

    fun clearSuggestions() {
        searchSuggestions = emptyList()
    }

    // --- URLs ---
    fun getDownloadUrl(itemId: String): String = getStorageUrlsUseCase.downloadUrl(itemId)
    fun getThumbnailUrl(itemId: String, size: String = "medium"): String =
        getStorageUrlsUseCase.thumbnailUrl(itemId, size)
    fun getPreviewUrl(itemId: String): String = getStorageUrlsUseCase.previewUrl(itemId)
    fun getDownloadZipUrl(): String = getStorageUrlsUseCase.batchDownloadZipUrl()

    // --- Error / share ---
    fun clearError() {
        error = null
    }
    fun clearShareError() {
        shareError = null
    }
    fun clearLastCreatedShare() {
        lastCreatedShare = null
    }

    fun createShare(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ) {
        viewModelScope.launch {
            isCreatingShare = true
            shareError = null
            when (
                val result = createShareUseCase(
                    itemId = itemId,
                    expiresInDays = expiresInDays,
                    password = password,
                    maxDownloads = maxDownloads,
                )
            ) {
                is ApiResult.Success -> lastCreatedShare = result.data
                is ApiResult.Error -> shareError = result.message
                is ApiResult.NetworkError -> shareError = result.message
            }
            isCreatingShare = false
        }
    }

    fun getShareUrl(shareLink: ShareLink): String = getShareUrlUseCase(shareLink.token)
}
