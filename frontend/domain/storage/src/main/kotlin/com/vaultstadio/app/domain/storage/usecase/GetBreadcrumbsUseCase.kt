/**
 * Get Breadcrumbs Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.Breadcrumb

/**
 * Use case for getting breadcrumbs for an item.
 */
interface GetBreadcrumbsUseCase {
    suspend operator fun invoke(itemId: String): Result<List<Breadcrumb>>
}
