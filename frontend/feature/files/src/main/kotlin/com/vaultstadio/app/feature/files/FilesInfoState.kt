package com.vaultstadio.app.feature.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Activity belongs to the current visible item; closing or changing the panel retires earlier work. */
internal class FilesInfoState(private val scope: CoroutineScope, private val getActivity: GetItemActivityUseCase) {
    var visible by mutableStateOf(false)
        private set
    var item by mutableStateOf<StorageItem?>(null)
        private set
    var activity by mutableStateOf<List<Activity>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    private var revision = 0L
    private var job: Job? = null

    fun show(selected: StorageItem) {
        item = selected
        visible = true
        activity = emptyList()
        load(selected.id)
    }

    fun hide() {
        revision++
        job?.cancel()
        visible = false
        item = null
        activity = emptyList()
        loading = false
    }

    fun update(updated: StorageItem) {
        if (item?.id == updated.id) item = updated
    }

    fun load(itemId: String, limit: Int = 10) {
        val identity = ++revision
        job?.cancel()
        job = scope.launch {
            loading = true
            try {
                val result = getActivity(itemId, limit)
                if (identity == revision && visible && item?.id == itemId) {
                    activity = if (result is Result.Success) result.data else emptyList()
                }
            } finally {
                if (identity == revision) loading = false
            }
        }
    }
}
