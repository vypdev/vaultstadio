package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository

/** Every public operation requires an explicit actor and current canonical ownership. */
class FileVersionService(
    private val versionRepository: FileVersionRepository,
    itemRepository: StorageItemRepository,
    storageBackend: StorageBackend,
    scopeRepository: VersionItemScopeRepository,
    contentReferences: VersionContentReferenceRepository,
) {
    private val access = VersionAccess(itemRepository, scopeRepository)
    private val reads = VersionReads(versionRepository)
    private val mutations = VersionMutations(versionRepository, reads, storageBackend, contentReferences)
    private val diffs = VersionDiffGenerator(storageBackend)

    suspend fun createVersion(
        input: CreateVersionInput,
        userId: String,
    ): Either<StorageException, FileVersion> =
        access.item(input.itemId, userId).flatMap { mutations.create(it, input, userId) }

    suspend fun restoreVersion(
        input: RestoreVersionInput,
        userId: String,
    ): Either<StorageException, FileVersion> =
        access.item(input.itemId, userId).flatMap { mutations.restore(it, input, userId) }

    suspend fun getHistory(itemId: String, userId: String): Either<StorageException, FileVersionHistory> =
        access.item(itemId, userId).flatMap { reads.history(it) }

    suspend fun getVersion(
        itemId: String,
        versionNumber: Int,
        userId: String,
    ): Either<StorageException, FileVersion> =
        access.item(itemId, userId).flatMap { reads.number(it, versionNumber) }

    suspend fun getLatestVersion(itemId: String, userId: String): Either<StorageException, FileVersion> =
        access.item(itemId, userId).flatMap { reads.latest(it) }

    suspend fun listVersions(
        itemId: String,
        userId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, List<FileVersion>> =
        access.item(itemId, userId).flatMap { reads.list(it, limit, offset) }

    suspend fun deleteVersion(versionId: String, userId: String): Either<StorageException, Unit> =
        access.version(versionId, userId).flatMap { mutations.delete(it, versionId) }

    suspend fun applyRetentionPolicy(
        itemId: String,
        userId: String,
        policy: VersionRetentionPolicy = VersionRetentionPolicy.DEFAULT,
    ): Either<StorageException, List<String>> =
        access.item(itemId, userId).flatMap { mutations.retention(it, policy) }

    suspend fun compareVersions(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
        userId: String,
    ): Either<StorageException, VersionDiff> =
        access.item(itemId, userId).flatMap { item ->
            reads.number(item, fromVersion).flatMap { from ->
                reads.number(item, toVersion).flatMap { to -> diffs.generate(item, from, to) }
            }
        }

    suspend fun getVersionCount(itemId: String, userId: String): Either<StorageException, Int> =
        access.item(itemId, userId).flatMap { versionRepository.countVersions(it.id) }

    suspend fun getTotalVersionSize(itemId: String, userId: String): Either<StorageException, Long> =
        access.item(itemId, userId).flatMap { versionRepository.getTotalVersionSize(it.id) }

    suspend fun cleanupVersionsForItem(itemId: String, userId: String): Either<StorageException, Int> =
        access.item(itemId, userId).flatMap { mutations.cleanup(it) }
}
