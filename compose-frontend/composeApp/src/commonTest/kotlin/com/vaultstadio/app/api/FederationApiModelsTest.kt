/**
 * VaultStadio Federation API Models Tests
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.domain.model.CreateFederatedShareRequest
import com.vaultstadio.app.domain.model.LinkIdentityRequest
import com.vaultstadio.app.domain.model.SharePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FederationApiModelsTest {

    @Test
    fun testCreateFederatedShareRequestCreation() {
        val request = CreateFederatedShareRequest(
            itemId = "item123",
            targetInstance = "vault.example.com",
            targetUserId = "user@example.com",
            permissions = listOf(SharePermission.READ, SharePermission.WRITE),
            expiresInDays = 30,
        )

        assertEquals("item123", request.itemId)
        assertEquals("vault.example.com", request.targetInstance)
        assertEquals(2, request.permissions.size)
        assertEquals(30, request.expiresInDays)
    }

    @Test
    fun testCreateFederatedShareRequestDefaults() {
        val request = CreateFederatedShareRequest(
            itemId = "item123",
            targetInstance = "vault.example.com",
        )

        assertEquals(null, request.targetUserId)
        assertEquals(listOf(SharePermission.READ), request.permissions)
        assertEquals(null, request.expiresInDays)
    }

    @Test
    fun testCreateFederatedShareRequestWithAllPermissions() {
        val request = CreateFederatedShareRequest(
            itemId = "item123",
            targetInstance = "vault.example.com",
            permissions = SharePermission.entries.toList(),
        )

        assertEquals(5, request.permissions.size)
        assertTrue(request.permissions.contains(SharePermission.READ))
        assertTrue(request.permissions.contains(SharePermission.WRITE))
        assertTrue(request.permissions.contains(SharePermission.DELETE))
        assertTrue(request.permissions.contains(SharePermission.SHARE))
        assertTrue(request.permissions.contains(SharePermission.ADMIN))
    }

    @Test
    fun testLinkIdentityRequestCreation() {
        val request = LinkIdentityRequest(
            remoteUserId = "user123",
            remoteInstance = "vault.example.com",
            displayName = "John Doe",
        )

        assertEquals("user123", request.remoteUserId)
        assertEquals("vault.example.com", request.remoteInstance)
        assertEquals("John Doe", request.displayName)
    }

    @Test
    fun testLinkIdentityRequestWithMinimalData() {
        val request = LinkIdentityRequest(
            remoteUserId = "user456",
            remoteInstance = "other.example.com",
            displayName = "User 456",
        )

        assertEquals("user456", request.remoteUserId)
        assertEquals("User 456", request.displayName)
    }

    @Test
    fun testSharePermissionValues() {
        val permissions = SharePermission.entries

        assertEquals(5, permissions.size)
        assertTrue(permissions.contains(SharePermission.READ))
        assertTrue(permissions.contains(SharePermission.WRITE))
        assertTrue(permissions.contains(SharePermission.DELETE))
        assertTrue(permissions.contains(SharePermission.SHARE))
        assertTrue(permissions.contains(SharePermission.ADMIN))
    }

    @Test
    fun testSharePermissionRead() {
        val permission = SharePermission.READ
        assertEquals("READ", permission.name)
    }

    @Test
    fun testSharePermissionWrite() {
        val permission = SharePermission.WRITE
        assertEquals("WRITE", permission.name)
    }

    @Test
    fun testSharePermissionAdmin() {
        val permission = SharePermission.ADMIN
        assertEquals("ADMIN", permission.name)
    }
}
