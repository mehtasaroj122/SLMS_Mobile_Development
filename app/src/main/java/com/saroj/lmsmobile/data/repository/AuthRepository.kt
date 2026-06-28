package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.auth.ForgotPasswordRequest
import com.saroj.lmsmobile.data.models.auth.ForgotPasswordResponse
import com.saroj.lmsmobile.data.models.auth.LoginRequest
import com.saroj.lmsmobile.data.models.auth.LoginResponse
import com.saroj.lmsmobile.data.models.auth.ProfileResponse
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.network.NetworkMonitor
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.NetworkMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * AuthRepository handles all authentication-related API calls.
 *
 * Responsibilities:
 * - Login with email/password
 * - Logout
 * - Fetch user profile
 * - Handle authentication errors
 *
 * Uses Flow to provide reactive data streams to ViewModels.
 *
 * Usage:
 *   val repository = AuthRepository(apiService, tokenManager)
 *   repository.login("user@example.com", "password").collect { result ->
 *       when (result) {
 *           is NetworkResult.Success -> { ... }
 *           is NetworkResult.Error -> { ... }
 *       }
 *   }
 */
class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    /**
     * Performs login with email and password.
     *
     * Flow:
     * 1. Emit Loading state
     * 2. Call API with credentials
     * 3. On success: Save token and user info, emit Success
     * 4. On 401: Emit Error with "Invalid credentials"
     * 5. On error: Emit Error
     *
     * @param email User email
     * @param password User password
     * @return Flow<NetworkResult<LoginResponse>>
     */
    fun login(email: String, password: String): Flow<NetworkResult<LoginResponse>> = flow {
        emit(NetworkResult.Loading())
        if (!NetworkMonitor.isCurrentlyOnline()) {
            emit(NetworkResult.Error(NetworkMessages.OFFLINE_LOGIN))
            return@flow
        }

        try {
            val response = apiService.login(LoginRequest(email, password))
            android.util.Log.d("AuthRepository", "Login response code: ${response.code()}")

            if (response.isSuccessful) {
                val loginResponse = response.body()
                android.util.Log.d("AuthRepository", "Login successful: $loginResponse")
                if (loginResponse != null) {
                    val normalizedRole = Constants.normalizeRole(loginResponse.user.role)
                    // Save token and user info to DataStore
                    tokenManager.saveToken(loginResponse.access_token)
                    tokenManager.saveUserInfo(
                        userId = loginResponse.user.id.toString(),
                        userName = loginResponse.user.name,
                        userEmail = loginResponse.user.email,
                        userRole = normalizedRole
                    )
                    emit(NetworkResult.Success(loginResponse.copy(user = loginResponse.user.copy(role = normalizedRole))))
                } else {
                    emit(NetworkResult.Error("Empty response from server", response.code()))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthRepository", "Login failed: $errorBody")
                when (response.code()) {
                    Constants.HTTP_UNAUTHORIZED -> {
                        // For login, 401 means invalid credentials, not session expired
                        emit(NetworkResult.Error("Invalid email or password", response.code()))
                    }
                    Constants.HTTP_UNPROCESSABLE_ENTITY -> {
                        emit(NetworkResult.Error("Please check your email and password", response.code()))
                    }
                    else -> emit(NetworkResult.Error(
                        "Server error: ${response.code()}",
                        response.code()
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Login exception: ${e.message}")
            emit(NetworkResult.Error(e.toRepositoryMessage()))
        }
    }.catch { e ->
        android.util.Log.e("AuthRepository", "Login catch: ${e.message}")
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun forgotPassword(email: String): Flow<NetworkResult<ForgotPasswordResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow

        val response = apiService.forgotPassword(ForgotPasswordRequest(email))
        if (response.isSuccessful) {
            val body = response.body()
            when {
                body == null -> emit(NetworkResult.Error("Empty response from server", response.code()))
                body.success -> emit(NetworkResult.Success(body))
                else -> emit(
                    NetworkResult.Error(
                        body.message?.takeIf { it.isNotBlank() }
                            ?: "We can't find a user with that email address.",
                        response.code()
                    )
                )
            }
            return@flow
        }

        val parsedError = parseError(response.errorBody()?.string())
        emit(
            NetworkResult.Error(
                parsedError.firstMessage()
                    ?: response.message().takeIf { it.isNotBlank() }
                    ?: "We can't find a user with that email address.",
                response.code()
            )
        )
    }.catch { e ->
        android.util.Log.e("AuthRepository", "Forgot password error: ${e.message}")
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    /**
     * Performs logout by calling the logout endpoint and clearing local data.
     *
     * @return Flow<NetworkResult<Any>>
     */
    fun logout(): Flow<NetworkResult<Any>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.logout()

            // Always clear local data on logout, regardless of response
            tokenManager.clearAllData()

            if (response.isSuccessful) {
                emit(NetworkResult.Success(Any()))
            } else {
                emit(NetworkResult.Error(
                    response.message() ?: Constants.ERROR_UNKNOWN,
                    response.code()
                ))
            }
        } catch (e: Exception) {
            // Clear local data even if network call fails
            tokenManager.clearAllData()
            emit(NetworkResult.Error(e.toRepositoryMessage()))
        }
    }.catch { e ->
        tokenManager.clearAllData()
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    /**
     * Fetches the current user's profile.
     *
     * @return Flow<NetworkResult<ProfileResponse>>
     */
    fun getProfile(): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow

        try {
            val response = apiService.getProfile()

            if (response.isSuccessful) {
                val profileResponse = response.body()
                if (profileResponse != null) {
                    emit(NetworkResult.Success(profileResponse))
                } else {
                    emit(NetworkResult.Error("Empty response from server", response.code()))
                }
            } else {
                when (response.code()) {
                    Constants.HTTP_UNAUTHORIZED -> {
                        tokenManager.clearToken()
                        emit(NetworkResult.Unauthorized())
                    }
                    else -> emit(NetworkResult.Error(
                        response.message() ?: Constants.ERROR_UNKNOWN,
                        response.code()
                    ))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.toRepositoryMessage()))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    private fun parseError(rawError: String?): ErrorResponse {
        if (rawError.isNullOrBlank()) return ErrorResponse()

        return runCatching {
            gson.fromJson(rawError, ErrorResponse::class.java)
        }.getOrElse {
            ErrorResponse(message = rawError.take(180))
        }
    }

    private fun ErrorResponse.firstMessage(): String? {
        return errors
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()
            ?: message?.takeIf { it.isNotBlank() }
    }
}

