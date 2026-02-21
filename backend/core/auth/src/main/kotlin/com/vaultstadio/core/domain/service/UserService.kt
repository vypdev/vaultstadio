/**
 * VaultStadio User Service
 *
 * Service for user management and authentication.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.UserEvent
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserSession
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.auth.repository.SessionRepository
import com.vaultstadio.domain.auth.repository.UserQuery
import com.vaultstadio.domain.auth.repository.UserRepository
import com.vaultstadio.domain.common.exception.AuthenticationException
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageQuota
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}

/**
 * Input for user registration.
 */
data class RegisterUserInput(
    val email: String,
    val username: String,
    val password: String,
    val role: UserRole = UserRole.USER,
)

/**
 * Input for user login.
 */
data class LoginInput(
    val email: String,
    val password: String,
    val ipAddress: String?,
    val userAgent: String?,
)

/**
 * Result of login.
 */
data class LoginResult(
    val user: User,
    val sessionToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

/**
 * Result of token refresh.
 */
data class RefreshResult(
    val user: User,
    val sessionToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

/**
 * Input for updating user profile.
 */
data class UpdateUserInput(
    val userId: String,
    val username: String? = null,
    val avatarUrl: String? = null,
)

/**
 * Password hasher interface.
 */
interface PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}

/**
 * Service for user management.
 */
class UserService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordHasher: PasswordHasher,
    private val eventBus: EventBus,
    private val sessionDuration: Duration = 7.days,
) {

    private val secureRandom = SecureRandom()

    /**
     * Registers a new user.
     */
    suspend fun register(input: RegisterUserInput): Either<StorageException, User> {
        if (!isValidEmail(input.email)) {
            return ValidationException("Invalid email format").left()
        }

        if (!isStrongPassword(input.password)) {
            return ValidationException("Password must be at least 8 characters with letters and numbers").left()
        }

        when (val result = userRepository.existsByEmail(input.email)) {
            is Either.Left -> return result
            is Either.Right -> {
                if (result.value) {
                    return ValidationException("Email already registered").left()
                }
            }
        }

        when (val result = userRepository.existsByUsername(input.username)) {
            is Either.Left -> return result
            is Either.Right -> {
                if (result.value) {
                    return ValidationException("Username already taken").left()
                }
            }
        }

        val now = Clock.System.now()
        val user = User(
            id = UUID.randomUUID().toString(),
            email = input.email.lowercase(),
            username = input.username,
            passwordHash = passwordHasher.hash(input.password),
            role = input.role,
            status = UserStatus.ACTIVE,
            quotaBytes = 10L * 1024 * 1024 * 1024, // 10GB default
            avatarUrl = null,
            preferences = null,
            lastLoginAt = null,
            createdAt = now,
            updatedAt = now,
        )

        return userRepository.create(user).also { result ->
            if (result.isRight()) {
                val created = (result as Either.Right).value
                eventBus.publish(UserEvent.Created(userId = null, user = created.toInfo()))
            }
        }
    }

    /**
     * Logs in a user.
     */
    suspend fun login(input: LoginInput): Either<StorageException, LoginResult> {
        val user = when (val result = userRepository.findByEmail(input.email)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return AuthenticationException("Invalid email or password").left()
                found
            }
        }

        if (!passwordHasher.verify(input.password, user.passwordHash)) {
            return AuthenticationException("Invalid email or password").left()
        }

        if (user.status != UserStatus.ACTIVE) {
            return AuthorizationException("Account is not active").left()
        }

        val sessionToken = generateSecureToken()
        val refreshToken = generateSecureToken()
        val now = Clock.System.now()
        val expiresAt = now + sessionDuration

        val session = UserSession(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            tokenHash = hashToken(sessionToken),
            refreshTokenHash = hashToken(refreshToken),
            ipAddress = input.ipAddress,
            userAgent = input.userAgent,
            expiresAt = expiresAt,
            createdAt = now,
            lastActivityAt = now,
        )

        when (val result = sessionRepository.create(session)) {
            is Either.Left -> return result
            is Either.Right -> { /* continue */ }
        }

        userRepository.updateLastLogin(user.id)
        eventBus.publish(UserEvent.LoggedIn(userId = user.id, ipAddress = input.ipAddress, userAgent = input.userAgent))

        return LoginResult(
            user = user,
            sessionToken = sessionToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        ).right()
    }

    /**
     * Refreshes a session using a refresh token.
     * Implements token rotation: old refresh token is invalidated and a new one is issued.
     */
    suspend fun refreshSession(refreshToken: String): Either<StorageException, RefreshResult> {
        val refreshTokenHash = hashToken(refreshToken)

        val session = when (val result = sessionRepository.findByRefreshTokenHash(refreshTokenHash)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return AuthenticationException("Invalid refresh token").left()
                found
            }
        }

        // Check if session is expired (we allow refresh even if access token expired, but not the session)
        if (session.expiresAt < Clock.System.now()) {
            sessionRepository.delete(session.id)
            return AuthenticationException("Session expired, please login again").left()
        }

        val user = when (val result = userRepository.findById(session.userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return AuthenticationException("User not found").left()
                found
            }
        }

        if (user.status != UserStatus.ACTIVE) {
            return AuthorizationException("Account is not active").left()
        }

        // Generate new tokens (token rotation)
        val newSessionToken = generateSecureToken()
        val newRefreshToken = generateSecureToken()
        val now = Clock.System.now()
        val newExpiresAt = now + sessionDuration

        // Update session with new tokens
        val updatedSession = session.copy(
            tokenHash = hashToken(newSessionToken),
            refreshTokenHash = hashToken(newRefreshToken),
            expiresAt = newExpiresAt,
            lastActivityAt = now,
        )

        when (val result = sessionRepository.update(updatedSession)) {
            is Either.Left -> return result
            is Either.Right -> { /* continue */ }
        }

        logger.debug { "Session refreshed for user ${user.id}" }

        return RefreshResult(
            user = user,
            sessionToken = newSessionToken,
            refreshToken = newRefreshToken,
            expiresAt = newExpiresAt,
        ).right()
    }

    /**
     * Validates a session token and returns the user.
     */
    suspend fun validateSession(token: String): Either<StorageException, User> {
        val tokenHash = hashToken(token)

        val session = when (val result = sessionRepository.findByTokenHash(tokenHash)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return AuthenticationException("Invalid session").left()
                found
            }
        }

        if (session.expiresAt < Clock.System.now()) {
            sessionRepository.delete(session.id)
            return AuthenticationException("Session expired").left()
        }

        val user = when (val result = userRepository.findById(session.userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return AuthenticationException("User not found").left()
                found
            }
        }

        sessionRepository.updateLastActivity(session.id)

        return user.right()
    }

    /**
     * Logs out a user by invalidating their session.
     */
    suspend fun logout(token: String): Either<StorageException, Unit> {
        val tokenHash = hashToken(token)

        val session = when (val result = sessionRepository.findByTokenHash(tokenHash)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }

        return if (session != null) {
            sessionRepository.delete(session.id)
        } else {
            Unit.right()
        }
    }

    /**
     * Logs out all sessions for a user.
     */
    suspend fun logoutAll(userId: String): Either<StorageException, Unit> {
        return sessionRepository.deleteByUserId(userId)
    }

    /**
     * Gets quota for a user.
     */
    suspend fun getQuota(userId: String): Either<StorageException, StorageQuota> {
        val user = when (val result = userRepository.findById(userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("User not found").left()
            }
        }

        // Calculate used storage - simplified version
        return StorageQuota(
            userId = userId,
            usedBytes = 0L, // Would be calculated from storage items
            quotaBytes = user.quotaBytes,
            fileCount = 0L,
            folderCount = 0L,
        ).right()
    }

    /**
     * Updates user profile.
     */
    suspend fun updateUser(input: UpdateUserInput): Either<StorageException, User> {
        val user = when (val result = userRepository.findById(input.userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("User not found").left()
            }
        }

        if (input.username != null && input.username != user.username) {
            when (val result = userRepository.existsByUsername(input.username)) {
                is Either.Left -> return result
                is Either.Right -> {
                    if (result.value) {
                        return ValidationException("Username already taken").left()
                    }
                }
            }
        }

        val updatedUser = user.copy(
            username = input.username ?: user.username,
            avatarUrl = input.avatarUrl ?: user.avatarUrl,
            updatedAt = Clock.System.now(),
        )

        return userRepository.update(updatedUser)
    }

    /**
     * Changes a user's password.
     */
    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String,
    ): Either<StorageException, Unit> {
        val user = when (val result = userRepository.findById(userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("User not found").left()
            }
        }

        if (!passwordHasher.verify(currentPassword, user.passwordHash)) {
            return AuthenticationException("Current password is incorrect").left()
        }

        if (!isStrongPassword(newPassword)) {
            return ValidationException("Password must be at least 8 characters with letters and numbers").left()
        }

        val updatedUser = user.copy(
            passwordHash = passwordHasher.hash(newPassword),
            updatedAt = Clock.System.now(),
        )

        when (val result = userRepository.update(updatedUser)) {
            is Either.Left -> return result
            is Either.Right -> { /* continue */ }
        }

        sessionRepository.deleteByUserId(userId)

        return Unit.right()
    }

    /**
     * Gets public user info.
     */
    suspend fun getUserInfo(userId: String): Either<StorageException, UserInfo> {
        val user = when (val result = userRepository.findById(userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("User not found").left()
            }
        }

        return user.toInfo().right()
    }

    /**
     * Lists users (admin only).
     */
    suspend fun listUsers(
        adminId: String,
        query: UserQuery,
    ): Either<StorageException, PagedResult<User>> {
        // Verify admin
        val admin = when (val result = userRepository.findById(adminId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("Admin not found").left()
            }
        }

        if (admin.role != UserRole.ADMIN) {
            return AuthorizationException("Admin access required").left()
        }

        return userRepository.query(query)
    }

    /**
     * Updates user quota (admin only).
     */
    suspend fun updateQuota(
        userId: String,
        quotaBytes: Long?,
        adminId: String,
    ): Either<StorageException, User> {
        // Verify admin
        val admin = when (val result = userRepository.findById(adminId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("Admin not found").left()
            }
        }

        if (admin.role != UserRole.ADMIN) {
            return AuthorizationException("Admin access required").left()
        }

        val user = when (val result = userRepository.findById(userId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("User not found").left()
            }
        }

        val updatedUser = user.copy(
            quotaBytes = quotaBytes,
            updatedAt = Clock.System.now(),
        )

        return userRepository.update(updatedUser)
    }

    /**
     * Deletes a user (admin only).
     */
    suspend fun deleteUser(userId: String, adminId: String): Either<StorageException, Unit> {
        // Verify admin
        val admin = when (val result = userRepository.findById(adminId)) {
            is Either.Left -> return result
            is Either.Right -> {
                result.value ?: return ItemNotFoundException("Admin not found").left()
            }
        }

        if (admin.role != UserRole.ADMIN) {
            return AuthorizationException("Admin access required").left()
        }

        // Delete all sessions
        sessionRepository.deleteByUserId(userId)

        // Delete user
        return userRepository.delete(userId)
    }

    /**
     * Gets user by ID.
     */
    suspend fun getUser(userId: String): Either<StorageException, User> {
        return when (val result = userRepository.findById(userId)) {
            is Either.Left -> result
            is Either.Right -> {
                val user = result.value
                if (user == null) {
                    ItemNotFoundException("User not found").left()
                } else {
                    user.right()
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isStrongPassword(password: String): Boolean {
        return password.length >= 8 &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Extension to convert User to UserInfo.
 */
fun User.toInfo() = UserInfo(
    id = id,
    email = email,
    username = username,
    role = role,
    status = status,
    quotaBytes = quotaBytes,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)
