/**
 * VaultStadio Federation API DTO and domain model tests.
 */

package com.vaultstadio.app.api

import com.vaultstadio.app.data.federation.dto.CreateFederatedShareRequestDTO
import com.vaultstadio.app.data.federation.dto.LinkIdentityRequestDTO
import com.vaultstadio.app.domain.federation.model.SharePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FederationApiModelsTest {

    @Test
    fun testCreateFederatedShareRequestCreation() {
        val request = CreateFederatedShareRequestDTO(
            itemId = "item123",
            targetInstance = "vault.example.com",
            targetUserId = "user@example.com",
            permissions = listOf("READ", "WRITE"),
            expiresInDays = 30,
        )

        assertEquals("item123", request.itemId)
        assertEquals("vault.example.com", request.targetInstance)
        assertEquals(2, request.permissions.size)
        assertEquals(30, request.expiresInDays)
    }

    @Test
    fun testCreateFederatedShareRequestDefaults() {
        val request = CreateFederatedShareRequestDTO(
            itemId = "item123",
            targetInstance = "vault.example.com",
        )

        assertEquals(null, request.targetUserId)
        assertEquals(listOf("READ"), request.permissions)
        assertEquals(null, request.expiresInDays)
    }

    @Test
    fun testCreateFederatedShareRequestWithAllPermissions() {
        val request = CreateFederatedShareRequestDTO(
            itemId = "item123",
            targetInstance = "vault.example.com",
            permissions = listOf("READ", "WRITE", "DELETE", "SHARE", "ADMIN"),
        )

        assertEquals(5, request.permissions.size)
        assertTrue(request.permissions.contains("READ"))
        assertTrue(request.permissions.contains("WRITE"))
        assertTrue(request.permissions.contains("DELETE"))
        assertTrue(request.permissions.contains("SHARE"))
        assertTrue(request.permissions.contains("ADMIN"))
    }

    @Test
    fun testLinkIdentityRequestCreation() {
        val request = LinkIdentityRequestDTO(
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
        val request = LinkIdentityRequestDTO(
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
