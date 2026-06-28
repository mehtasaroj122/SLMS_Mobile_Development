package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.profile.ChangePasswordRequest
import com.saroj.lmsmobile.data.models.profile.DeleteAccountRequest
import com.saroj.lmsmobile.data.models.profile.ProfileUpdateRequest
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.staff.model.StaffProfileUiModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StaffProfileRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getProfile(): Flow<NetworkResult<StaffProfileUiModel>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_PROFILE, StaffProfileUiModel::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getProfileJson()
        if (response.isSuccessful) {
            val profile = parseProfile(response.body())
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, profile)
            emit(NetworkResult.Success(profile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun updateProfile(profile: StaffProfileUiModel): Flow<NetworkResult<StaffProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.updateProfile(
            ProfileUpdateRequest(
                name = profile.name,
                email = null,
                phone = profile.phone.takeIf { it.isNotBlank() },
                gender = null,
                address = profile.address.takeIf { it.isNotBlank() }
            )
        )

        if (response.isSuccessful) {
            val updatedProfile = parseProfile(response.body(), fallback = profile)
            val existingUserId = tokenManager.getUserId().firstOrNull() ?: "0"
            tokenManager.saveUserInfo(
                userId = existingUserId,
                userName = updatedProfile.name,
                userEmail = updatedProfile.email,
                userRole = updatedProfile.role.lowercase(Locale.US)
            )
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, updatedProfile)
            emit(NetworkResult.Success(updatedProfile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.changeProfilePassword(
            ChangePasswordRequest(
                current_password = currentPassword,
                password = newPassword,
                password_confirmation = confirmPassword
            )
        )

        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseMessage(response.body(), "Password changed successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun uploadPhoto(
        file: File,
        fallback: StaffProfileUiModel?
    ): Flow<NetworkResult<StaffProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val requestBody = file.asRequestBody(resolveMediaType(file.name).toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)
        val response = apiService.uploadProfilePhoto(part)

        if (response.isSuccessful) {
            val updatedProfile = parseProfile(response.body(), fallback = fallback)
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, updatedProfile)
            emit(NetworkResult.Success(updatedProfile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun removePhoto(currentProfile: StaffProfileUiModel): Flow<NetworkResult<StaffProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.removeProfilePhoto()
        if (response.isSuccessful) {
            val updatedProfile = parseProfile(response.body(), fallback = currentProfile.copy(profilePhotoUrl = null))
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, updatedProfile)
            emit(NetworkResult.Success(updatedProfile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun deleteAccount(password: String, confirmation: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.deleteProfileAccount(
            DeleteAccountRequest(
                current_password = password,
                confirmation_text = confirmation
            )
        )
        if (response.isSuccessful) {
            tokenManager.clearAllData()
            emit(NetworkResult.Success(parseMessage(response.body(), "Account deactivated successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun logout(): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.logout()
        tokenManager.clearAllData()
        if (response.isSuccessful || response.code() == Constants.HTTP_UNAUTHORIZED) {
            emit(NetworkResult.Success("Logged out successfully"))
        } else {
            emit(NetworkResult.Success("Logged out locally"))
        }
    }.catch {
        tokenManager.clearAllData()
        emit(NetworkResult.Success("Logged out locally"))
    }

    private fun parseProfile(
        root: JsonElement?,
        fallback: StaffProfileUiModel? = null
    ): StaffProfileUiModel {
        val rootObject = root.asObjectOrNull()
        val dataObject = rootObject?.objectValue("data")
        val user = rootObject?.objectValue("user")
            ?: dataObject?.objectValue("user")
            ?: dataObject
            ?: rootObject
            ?: JsonObject()
        val staff = user.objectValue("staff", "staff_profile", "profile")
            ?: dataObject?.objectValue("staff", "staff_profile", "profile")
            ?: rootObject?.objectValue("staff", "staff_profile", "profile")
            ?: JsonObject()

        val photoUrl = user.stringValue("profile_photo_url", "photo_url", "profile_photo", "avatar")
            ?: dataObject?.stringValue("profile_photo_url", "photo_url", "profile_photo", "avatar")
            ?: rootObject?.stringValue("profile_photo_url", "photo_url", "profile_photo", "avatar")
            ?: staff.stringValue("profile_photo_url", "photo_url", "profile_photo", "avatar")
            ?: fallback?.profilePhotoUrl

        return StaffProfileUiModel(
            name = user.stringValue("name", "full_name", "fullName")
                ?: staff.stringValue("name", "full_name", "fullName")
                ?: fallback?.name
                ?: "-",
            email = user.stringValue("email")
                ?: staff.stringValue("email")
                ?: fallback?.email
                ?: "-",
            role = (user.stringValue("role", "user_role")
                ?: staff.stringValue("role", "user_role")
                ?: fallback?.role
                ?: "-").titleCase(),
            username = user.stringValue("username", "user_name")
                ?: staff.stringValue("username", "user_name")
                ?: fallback?.username
                ?: "-",
            staffId = staff.stringValue("staff_id", "staffId", "employee_id", "employeeId")
                ?: user.stringValue("staff_id", "staffId", "employee_id", "employeeId")
                ?: fallback?.staffId
                ?: "-",
            department = staff.stringValue("department", "department_name", "departmentName")
                ?: user.stringValue("department", "department_name", "departmentName")
                ?: fallback?.department
                ?: "-",
            designation = staff.stringValue("designation", "position", "job_title", "jobTitle")
                ?: user.stringValue("designation", "position", "job_title", "jobTitle")
                ?: fallback?.designation
                ?: "-",
            memberSince = formatDate(
                staff.stringValue("join_date", "joinDate", "member_since", "memberSince", "created_at", "createdAt")
                    ?: user.stringValue("member_since", "memberSince", "joined_at", "joinedAt", "created_at", "createdAt")
                    ?: dataObject?.stringValue("member_since", "memberSince", "created_at", "createdAt")
            ).takeUnless { it == "-" } ?: fallback?.memberSince ?: "-",
            lastLogin = formatDateTime(
                user.stringValue(
                    "last_login",
                    "last_login_at",
                    "lastLogin",
                    "lastLoginAt",
                    "last_seen_at",
                    "lastSeenAt",
                    "current_login_at",
                    "currentLoginAt"
                ) ?: dataObject?.stringValue(
                    "last_login",
                    "last_login_at",
                    "lastLogin",
                    "lastLoginAt",
                    "last_seen_at",
                    "lastSeenAt",
                    "current_login_at",
                    "currentLoginAt"
                )
            ).takeUnless { it == "-" } ?: fallback?.lastLogin ?: "-",
            phone = user.stringValue("phone", "phone_number", "mobile")
                ?: staff.stringValue("phone", "phone_number", "mobile")
                ?: fallback?.phone
                ?: "",
            address = user.stringValue("address")
                ?: staff.stringValue("address")
                ?: fallback?.address
                ?: "",
            profilePhotoUrl = photoUrl
        )
    }

    private suspend fun <T> handleError(response: Response<*>): NetworkResult<T> {
        val message = parseErrorMessage(response)
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            Constants.HTTP_NOT_FOUND -> NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code())
            Constants.HTTP_UNPROCESSABLE_ENTITY -> NetworkResult.Error(message.ifBlank { Constants.ERROR_VALIDATION }, response.code())
            else -> NetworkResult.Error(message, response.code())
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) return response.message().ifBlank { Constants.ERROR_UNKNOWN }

        val parsed = runCatching { gson.fromJson(rawError, ErrorResponse::class.java) }.getOrNull()
        val firstValidationError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(160)
    }

    private fun parseMessage(root: JsonElement?, fallback: String): String {
        return root.asObjectOrNull()?.stringValue("message", "status") ?: fallback
    }

    private fun resolveMediaType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun formatDate(rawDate: String?): String {
        val date = parseDate(rawDate) ?: return rawDate?.take(10)?.takeIf { it.isNotBlank() } ?: "-"
        return SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(date)
    }

    private fun formatDateTime(rawDate: String?): String {
        val date = parseDate(rawDate) ?: return rawDate?.takeIf { it.isNotBlank() } ?: "-"
        return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(date)
    }

    private fun parseDate(rawDate: String?): java.util.Date? {
        if (rawDate.isNullOrBlank()) return null
        val normalized = rawDate.trim().replace(Regex("\\.\\d+Z$"), "Z")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)
            }.getOrNull()
        }
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? {
        return if (this != null && isJsonObject) asJsonObject else null
    }

    private fun JsonObject.objectValue(vararg keys: String): JsonObject? {
        return keys.firstNotNullOfOrNull { key -> get(key).asObjectOrNull() }
    }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }

    private fun String.titleCase(): String {
        if (this == "-") return this
        return replace("_", " ").split(" ").filter { it.isNotBlank() }.joinToString(" ") {
            it.lowercase(Locale.US).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
    }

    private companion object {
        const val CACHE_KEY_PROFILE = "staff:profile"
    }
}
