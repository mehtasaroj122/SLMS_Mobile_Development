package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.profile.ChangePasswordRequest
import com.saroj.lmsmobile.data.models.profile.ProfileUpdateRequest
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.student.model.DeleteEligibilityUiModel
import com.saroj.lmsmobile.ui.student.model.StudentProfileUiModel
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

class StudentProfileRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getProfile(): Flow<NetworkResult<StudentProfileUiModel>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_PROFILE, StudentProfileUiModel::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        val response = apiService.getProfileJson()
        if (response.isSuccessful) {
            val profile = parseProfile(response.body())
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, profile)
            emit(NetworkResult.Success(profile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun updateProfile(profile: StudentProfileUiModel): Flow<NetworkResult<StudentProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.updateProfile(
            ProfileUpdateRequest(
                name = profile.name,
                email = profile.email,
                phone = profile.phone.takeIf { it.isNotBlank() },
                gender = profile.gender.toBackendGenderValue(),
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
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.changePassword(
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
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getDeleteEligibility(): Flow<NetworkResult<DeleteEligibilityUiModel>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getProfileDeleteEligibility()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseDeleteEligibility(response.body())))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun uploadPhoto(
        file: File,
        fallback: StudentProfileUiModel?
    ): Flow<NetworkResult<StudentProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
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
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun removePhoto(currentProfile: StudentProfileUiModel): Flow<NetworkResult<StudentProfileUiModel>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.removeProfilePhoto()
        if (response.isSuccessful) {
            val updatedProfile = parseProfile(response.body(), fallback = currentProfile.copy(profilePhotoUrl = null))
            LocalCacheProvider.cache?.write(CACHE_KEY_PROFILE, updatedProfile)
            emit(NetworkResult.Success(updatedProfile))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun deleteAccount(): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.deleteProfile()
        if (response.isSuccessful) {
            tokenManager.clearAllData()
            emit(NetworkResult.Success(parseMessage(response.body(), "Account deleted successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private fun parseProfile(
        root: JsonElement?,
        fallback: StudentProfileUiModel? = null
    ): StudentProfileUiModel {
        val rootObject = root.asObjectOrNull()
        val dataObject = rootObject?.objectValue("data")
        val user = rootObject?.objectValue("user")
            ?: dataObject?.objectValue("user")
            ?: dataObject
            ?: rootObject
            ?: JsonObject()
        val student = user.objectValue("student", "profile", "student_profile")
            ?: dataObject?.objectValue("student", "profile", "student_profile")
            ?: rootObject?.objectValue("student", "profile", "student_profile")
            ?: JsonObject()

        val name = user.stringValue("name", "full_name", "fullName")
            ?: student.stringValue("name", "full_name", "fullName")
            ?: fallback?.name
            ?: "Student"
        val email = user.stringValue("email")
            ?: student.stringValue("email")
            ?: fallback?.email
            ?: "-"
        val role = user.stringValue("role", "user_role")
            ?: fallback?.role
            ?: "Student"
        val username = user.stringValue("username", "user_name")
            ?: student.stringValue("username", "student_username")
            ?: email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: fallback?.username
            ?: "-"
        val department = student.stringValue("department", "faculty", "department_name")
            ?: user.stringValue("department", "faculty", "department_name")
            ?: fallback?.department
            ?: "-"

        return StudentProfileUiModel(
            name = name,
            email = email,
            role = role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
            username = username,
            studentId = student.stringValue("student_id", "studentId", "roll_no", "rollNo", "roll_number")
                ?: user.stringValue("student_id", "studentId", "roll_no", "rollNo")
                ?: fallback?.studentId
                ?: "-",
            department = department,
            memberSince = formatDate(
                student.stringValue(
                    "member_since",
                    "memberSince",
                    "joined_at",
                    "joinedAt",
                    "registered_at",
                    "registeredAt",
                    "created_at",
                    "createdAt"
                ) ?: user.stringValue(
                    "member_since",
                    "memberSince",
                    "joined_at",
                    "joinedAt",
                    "registered_at",
                    "registeredAt",
                    "created_at",
                    "createdAt"
                ) ?: dataObject?.stringValue(
                    "member_since",
                    "memberSince",
                    "joined_at",
                    "joinedAt",
                    "registered_at",
                    "registeredAt",
                    "created_at",
                    "createdAt"
                )
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
                ) ?: student.stringValue(
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
            phone = student.stringValue("phone", "phone_number", "mobile")
                ?: user.stringValue("phone", "phone_number", "mobile")
                ?: fallback?.phone
                ?: "",
            gender = student.stringValue("gender")
                ?: user.stringValue("gender")
                ?: fallback?.gender
                ?: "",
            address = student.stringValue("address")
                ?: user.stringValue("address")
                ?: fallback?.address
                ?: "",
            profilePhotoUrl = user.stringValue("profile_photo_url", "profile_photo", "photo_url", "avatar")
                ?: student.stringValue("profile_photo_url", "profile_photo", "photo_url", "avatar")
                ?: fallback?.profilePhotoUrl
        )
    }

    private fun parseDeleteEligibility(root: JsonElement?): DeleteEligibilityUiModel {
        val rootObject = root.asObjectOrNull()
        val source = rootObject?.objectValue("data", "eligibility") ?: rootObject ?: JsonObject()
        val canDelete = source.booleanValue("can_delete", "canDelete", "eligible") ?: false
        val issuedBooks = source.intValue("issued_books", "issuedBooks", "active_issues", "borrowed_books")
        val activeRequests = source.intValue("active_requests", "activeRequests", "pending_requests")
        val pendingFines = source.stringValue("pending_fines", "pendingFines", "fine_amount", "fines")
            ?: formatCurrency(source.doubleValue("pending_fines_amount", "pendingFinesAmount", "fine_amount"))

        val reasons = source.arrayStrings("reasons", "messages").ifEmpty {
            buildList {
                if (issuedBooks > 0) add("Return all issued books")
                if (pendingFines != "₹0.00" && pendingFines != "₹0") add("Clear pending fines")
                if (activeRequests > 0) add("Resolve active requests")
            }
        }

        return DeleteEligibilityUiModel(
            canDelete = canDelete,
            issuedBooks = issuedBooks,
            pendingFines = pendingFines,
            activeRequests = activeRequests,
            reasons = reasons
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

    private fun String.toBackendGenderValue(): String? {
        return when (trim().lowercase(Locale.US)) {
            "male" -> "male"
            "female" -> "female"
            "other", "others" -> "other"
            else -> null
        }
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "₹%.2f", value)
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

    private fun JsonObject.intValue(vararg keys: String): Int {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asInt }.getOrNull()
            }
        } ?: 0
    }

    private fun JsonObject.doubleValue(vararg keys: String): Double {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asDouble }.getOrNull()
            }
        } ?: 0.0
    }

    private fun JsonObject.booleanValue(vararg keys: String): Boolean? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asBoolean }.getOrNull()
                    ?: runCatching { value.asInt == 1 }.getOrNull()
                    ?: runCatching {
                        value.asString.equals("true", ignoreCase = true) ||
                            value.asString.equals("yes", ignoreCase = true) ||
                            value.asString == "1"
                    }.getOrNull()
            }
        }
    }

    private fun JsonObject.arrayStrings(vararg keys: String): List<String> {
        return keys.firstNotNullOfOrNull { key ->
            val value = get(key)
            if (value?.isJsonArray == true) {
                value.asJsonArray.mapNotNull { element ->
                    element.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
                }
            } else {
                null
            }
        }.orEmpty()
    }

    private companion object {
        const val CACHE_KEY_PROFILE = "student:profile"
    }
}
