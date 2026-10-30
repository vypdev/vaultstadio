package com.vaultstadio.app.feature.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.CreateShareUseCase
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.ViewMode
import com.vaultstadio.app.domain.storage.usecase.BatchCopyUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchDeleteUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchMoveUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchStarUseCase
import com.vaultstadio.app.domain.storage.usecase.CreateFolderUseCase
import com.vaultstadio.app.domain.storage.usecase.DeleteItemUseCase
import com.vaultstadio.app.domain.storage.usecase.EmptyTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.GetBreadcrumbsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetFolderItemsUseCase
import com.vaultstadio.app.domain.storage.usecase.GetRecentUseCase
import com.vaultstadio.app.domain.storage.usecase.GetStarredUseCase
import com.vaultstadio.app.domain.storage.usecase.GetTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.RenameItemUseCase
import com.vaultstadio.app.domain.storage.usecase.RestoreItemUseCase
import com.vaultstadio.app.domain.storage.usecase.SearchUseCase
import com.vaultstadio.app.domain.storage.usecase.ToggleStarUseCase
import com.vaultstadio.app.domain.storage.usecase.TrashItemUseCase
import kotlinx.coroutines.launch

/**
 * ViewModel for file management screens (Files, Recent, Starred, Trash).
 * Loading and path resolution are delegated to [FilesLoader]; this class holds state and UI callbacks.
 */
class FilesViewModel(
    private val filesViewPreferences: FilesViewPreferences,
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
    private val mode: FilesMode,
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

    var currentFolderId by mutableStateOf<String?>(null)
        private set
    var currentFolderName by mutableStateOf<String?>(null)
        private set
    var breadcrumbs by mutableStateOf<List<Breadcrumb>>(emptyList())
        private set

    var items by mutableStateOf<List<StorageItem>>(emptyList())
        private set
    private var listLoading by mutableStateOf(false)
    private var pendingMutations by mutableStateOf(0)
    val isLoading: Boolean get() = listLoading || pendingMutations > 0
    var error by mutableStateOf<String?>(null)
        private set

    var viewMode by mutableStateOf(ViewMode.GRID)
        private set
    var sortField by mutableStateOf(SortField.TYPE)
        private set
    var sortOrder by mutableStateOf(SortOrder.ASC)
        private set

    var folderTotalItems by mutableStateOf(0L)
        private set
    var folderHasMore by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set

    var selectedItems by mutableStateOf<Set<String>>(emptySet())
        private set
    val isSelectionMode: Boolean get() = selectedItems.isNotEmpty()

    private val info = FilesInfoState(viewModelScope, getItemActivityUseCase)
    private val requests = FilesLoadingCoordinator(viewModelScope, loader)
    val showInfoPanel: Boolean get() = info.visible
    val selectedInfoItem: StorageItem? get() = info.item
    val itemActivity: List<Activity> get() = info.activity
    val isLoadingActivity: Boolean get() = info.loading

    private var displayedMode: FilesMode = mode

    var searchQuery by mutableStateOf("")
        private set
    var isSearching by mutableStateOf(false)
        private set
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())
        private set

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
        filesViewPreferences.getViewMode()?.let { value ->
            enumValues<ViewMode>().find { it.name == value }?.let { viewMode = it }
        }
        filesViewPreferences.getSortField()?.let { value ->
            enumValues<SortField>().find { it.name == value }?.let { sortField = it }
        }
        filesViewPreferences.getSortOrder()?.let { value ->
            enumValues<SortOrder>().find { it.name == value }?.let { sortOrder = it }
        }
    }

    fun navigateToFolder(folderId: String?, folderName: String? = null) {
        currentFolderId = folderId
        currentFolderName = folderName?.takeIf { it.isNotBlank() }
        clearSelection()
        if (mode == FilesMode.ALL) {
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
        if (mode != FilesMode.ALL || segments.isEmpty()) return
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

    fun loadItemsForMode(displayedMode: FilesMode) {
        this.displayedMode = displayedMode
        loadItems()
    }

    private fun loadContext() = FilesLoadContext(
        displayedMode,
        currentFolderId,
        currentFolderName,
        sortField,
        sortOrder,
    )

    private fun loadItems(preserveError: Boolean = false) {
        val context = loadContext()
        requests.load(
            context,
            started = {
                listLoading = true
                isLoadingMore = false
                if (!preserveError) error = null
            },
            completed = { result ->
                when (result) {
                    is LoadItemsResult.Success -> {
                        items = result.items
                        folderTotalItems = result.total
                        folderHasMore = result.hasMore
                        breadcrumbs = result.breadcrumbs
                        currentFolderName = result.currentFolderName
                    }
                    is LoadItemsResult.Error -> if (!preserveError || error == null) error = result.message
                }
            },
            finished = { listLoading = false },
        )
    }

    fun loadMoreFolderItems() {
        if (displayedMode != FilesMode.ALL || !folderHasMore || isLoading || isLoadingMore) return
        requests.more(
            loadContext(),
            items.size,
            started = { isLoadingMore = true },
            completed = { result ->
                when (result) {
                    is LoadMoreResult.Success -> {
                        items = items + result.newItems
                        folderHasMore = result.hasMore
                    }
                    is LoadMoreResult.Error -> error = result.message
                }
            },
            finished = { isLoadingMore = false },
        )
    }

    private fun runOp(
        block: suspend () -> Result<*>,
        onSuccess: () -> Unit = { loadItems() },
    ) {
        viewModelScope.launch {
            when (val result = block()) {
                is Result.Success -> onSuccess()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    private fun runOpWithLoading(
        block: suspend () -> Result<*>,
        onSuccess: () -> Unit = { loadItems() },
    ) {
        viewModelScope.launch {
            pendingMutations++
            try {
                when (val result = block()) {
                    is Result.Success -> onSuccess()
                    is Result.Error -> error = result.message
                    is Result.NetworkError -> error = result.message
                }
            } finally {
                pendingMutations--
            }
        }
    }

    private fun runBatchOp(loading: Boolean = true, block: suspend (List<String>) -> Result<BatchResult>) {
        val requested = selectedItems.toSet()
        if (requested.isEmpty()) return
        val context = loadContext()
        viewModelScope.launch {
            if (loading) pendingMutations++
            try {
                when (val result = block(requested.toList())) {
                    is Result.Success -> {
                        val outcome = result.data.forSelection(requested)
                        if (currentFolderId == context.folderId && displayedMode == context.mode) {
                            selectedItems = (selectedItems - requested) + (outcome.remaining intersect selectedItems)
                        }
                        if (outcome.error != null) error = outcome.error
                        loadItems(preserveError = outcome.error != null)
                    }
                    is Result.Error -> error = result.message
                    is Result.NetworkError -> error = result.message
                }
            } finally {
                if (loading) pendingMutations--
            }
        }
    }

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
                is Result.Success -> {
                    loadItems()
                    info.update(result.data)
                }
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun trashItem(itemId: String) = runOp(block = { trashItemUseCase(itemId) })
    fun restoreItem(itemId: String) = runOp(block = { restoreItemUseCase(itemId) })
    fun deleteItemPermanently(itemId: String) = runOp(block = { deleteItemUseCase(itemId) })

    fun emptyTrash(displayedMode: FilesMode) {
        runOpWithLoading(
            block = { emptyTrashUseCase() },
            onSuccess = { loadItemsForMode(displayedMode) },
        )
    }

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

    fun batchDelete(permanent: Boolean = false) = runBatchOp { batchDeleteUseCase(it, permanent) }
    fun batchMove(destinationId: String?) = runBatchOp { batchMoveUseCase(it, destinationId) }

    fun moveItemToFolder(itemId: String, destinationId: String?) {
        viewModelScope.launch {
            items = items.filter { it.id != itemId }
            when (val result = batchMoveUseCase(listOf(itemId), destinationId)) {
                is Result.Success -> {
                    val outcome = result.data.forSelection(setOf(itemId))
                    if (outcome.error != null) error = outcome.error
                    loadItems(preserveError = outcome.error != null)
                }
                is Result.Error -> {
                    error = result.message
                    loadItems(preserveError = true)
                }
                is Result.NetworkError -> {
                    error = result.message
                    loadItems(preserveError = true)
                }
            }
        }
    }

    fun batchCopy(destinationId: String?) = runBatchOp { batchCopyUseCase(it, destinationId) }
    fun batchStar(starred: Boolean) = runBatchOp(loading = false) { batchStarUseCase(it, starred) }

    fun showItemInfo(item: StorageItem) = info.show(item)
    fun hideItemInfo() = info.hide()
    fun loadItemActivity(itemId: String, limit: Int = 10) = info.load(itemId, limit)

    fun updateViewMode(mode: ViewMode) {
        viewMode = mode
        filesViewPreferences.setViewMode(mode.name)
    }

    fun updateSortField(field: SortField) {
        if (sortField != field) {
            sortField = field
            filesViewPreferences.setSortField(field.name)
            loadItems()
        }
    }

    fun updateSortOrder(order: SortOrder) {
        if (sortOrder != order) {
            sortOrder = order
            filesViewPreferences.setSortOrder(order.name)
            loadItems()
        }
    }

    fun search(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            loadItems()
            return
        }
        viewModelScope.launch {
            isSearching = true
            when (val result = searchUseCase(query)) {
                is Result.Success -> items = result.data.items
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
                is Result.Success -> searchSuggestions = result.data
                else -> { }
            }
        }
    }

    fun clearSuggestions() {
        searchSuggestions = emptyList()
    }

    fun getDownloadUrl(itemId: String): String = getStorageUrlsUseCase.downloadUrl(itemId)
    fun getThumbnailUrl(itemId: String, size: String = "medium"): String =
        getStorageUrlsUseCase.thumbnailUrl(itemId, size)
    fun getPreviewUrl(itemId: String): String = getStorageUrlsUseCase.previewUrl(itemId)
    fun getDownloadZipUrl(): String = getStorageUrlsUseCase.batchDownloadZipUrl()

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
                is Result.Success -> lastCreatedShare = result.data
                is Result.Error -> shareError = result.message
                is Result.NetworkError -> shareError = result.message
            }
            isCreatingShare = false
        }
    }

    fun getShareUrl(shareLink: ShareLink): String = getShareUrlUseCase(shareLink.token)
}
