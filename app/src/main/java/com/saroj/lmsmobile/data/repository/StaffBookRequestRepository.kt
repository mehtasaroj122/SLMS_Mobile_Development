package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestActionResult
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestStatus
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsPageUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsSummaryUiModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StaffBookRequestRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getSummary(): Flow<NetworkResult<StaffBookRequestsSummaryUiModel>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getStaffBookRequestSummary()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseSummary(response.body())))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getRequests(
        status: String,
        search: String,
        page: Int = 1,
        pageSize: Int = PAGE_SIZE
    ): Flow<NetworkResult<StaffBookRequestsPageUiModel>> = flow {
        emit(NetworkResult.Loading())
        val normalizedSearch = search.trim().takeIf { it.isNotBlank() }
        val response = apiService.getStaffBookRequests(
            status = status,
            search = normalizedSearch,
            page = page,
            pageSize = pageSize
        )
        if (response.isSuccessful) {
            val requests = extractRequestElements(response.body()).mapIndexed { index, element ->
                parseRequest(element, fallbackId = index + 1)
            }.sortedByDescending { it.sortDateMillis }
            emit(NetworkResult.Success(parseRequestsPage(response.body(), requests, page)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private fun parseRequestsPage(
        root: JsonElement?,
        requests: List<StaffBookRequestUiModel>,
        fallbackPage: Int
    ): StaffBookRequestsPageUiModel {
        val meta = root.asObjectOrNull()?.get("meta").asObjectOrNull()
            ?: root.asObjectOrNull()?.get("pagination").asObjectOrNull()
            ?: root.dataObject()?.get("meta").asObjectOrNull()
            ?: root.dataObject()?.get("pagination").asObjectOrNull()
        return StaffBookRequestsPageUiModel(
            requests = requests,
            currentPage = meta?.intValue("current_page", "currentPage", "page", fallback = fallbackPage)
                ?: fallbackPage,
            lastPage = meta?.intValue("last_page", "lastPage", "pages", fallback = fallbackPage)
                ?: fallbackPage
        )
    }

    fun approveRequest(request: StaffBookRequestUiModel): Flow<NetworkResult<StaffBookRequestActionResult>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.approveStaffBookRequest(request.id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseActionResult(response.body(), request, StaffBookRequestStatus.APPROVED)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun rejectRequest(request: StaffBookRequestUiModel): Flow<NetworkResult<StaffBookRequestActionResult>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.rejectStaffBookRequest(request.id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseActionResult(response.body(), request, StaffBookRequestStatus.REJECTED)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private fun parseSummary(root: JsonElement?): StaffBookRequestsSummaryUiModel {
        val source = root.dataObject() ?: root.asObjectOrNull() ?: JsonObject()
        return StaffBookRequestsSummaryUiModel(
            totalRequests = source.intValue("total", "total_requests", "totalRequests"),
            pendingRequests = source.intValue("pending", "pending_requests", "pendingRequests"),
            approvedRequests = source.intValue("approved", "approved_requests", "approvedRequests"),
            rejectedRequests = source.intValue("rejected", "rejected_requests", "rejectedRequests")
        )
    }

    private fun extractRequestElements(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()

        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            listOf("data", "requests", "book_requests", "items").forEach { key ->
                val nested = data.asJsonObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }

        val rootObject = root.asObjectOrNull() ?: return emptyList()
        listOf("requests", "book_requests", "items").forEach { key ->
            val nested = rootObject.get(key)
            if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
        }
        return emptyList()
    }

    private fun extractRequestElement(root: JsonElement?): JsonElement {
        if (root == null || root.isJsonNull) return JsonObject()
        if (root.isJsonObject) {
            val rootObject = root.asJsonObject
            val data = rootObject.get("data")
            if (data?.isJsonObject == true) return data
            listOf("request", "book_request", "item").forEach { key ->
                val nested = rootObject.get(key)
                if (nested?.isJsonObject == true) return nested
            }
        }
        return root
    }

    private fun parseRequest(element: JsonElement, fallbackId: Int): StaffBookRequestUiModel {
        val request = element.asObjectOrNull() ?: JsonObject()
        val student = request.objectValue("student", "student_data") ?: JsonObject()
        val book = request.objectValue("book", "book_data") ?: JsonObject()
        val rawStatus = request.stringValue("status", "request_status", "state")
        val status = parseStatus(rawStatus)
        val rawRequestDate = request.stringValue("request_date", "requested_at", "created_at", "createdAt")
        val rawProcessedDate = request.stringValue("processed_at", "processed_date", "processedAt", "processedDate")

        val studentName = student.stringValue("name", "full_name")
            ?: request.stringValue("student_name", "studentName")
            ?: "Unknown Student"
        val studentIdentifier = student.stringValue("student_id", "roll_no", "symbol_no")
            ?: request.stringValue("student_roll_no", "student_symbol_no", "student_number", "roll_no", "symbol_no")
            ?: "-"

        return StaffBookRequestUiModel(
            id = request.intValue("id", "request_id", "book_request_id", fallback = fallbackId),
            studentName = studentName,
            studentIdentifier = studentIdentifier,
            studentDepartment = student.stringValue("department", "faculty")
                ?: request.stringValue("department", "faculty")
                ?: "-",
            studentPhotoUrl = normalizeAssetUrl(
                student.stringValue("photo_url", "profile_photo_url", "photo", "profile_photo")
                    ?: request.stringValue("student_photo_url", "student_photo")
            ),
            bookTitle = book.stringValue("title", "book_title")
                ?: request.stringValue("book_title", "title")
                ?: "Untitled Book",
            author = book.stringValue("author", "book_author")
                ?: request.stringValue("author", "book_author")
                ?: "Unknown author",
            requestDate = formatDate(rawRequestDate),
            processedBy = request.stringValue("processed_by", "processed_by_name", "processedBy") ?: "-",
            processedAt = formatDate(rawProcessedDate),
            status = status,
            sortDateMillis = parseDateMillis(rawRequestDate)
        )
    }

    private fun parseActionResult(
        root: JsonElement?,
        original: StaffBookRequestUiModel,
        fallbackStatus: StaffBookRequestStatus
    ): StaffBookRequestActionResult {
        val rootObject = root.asObjectOrNull()
        val message = when (fallbackStatus) {
            StaffBookRequestStatus.APPROVED -> "Request accepted successfully"
            StaffBookRequestStatus.REJECTED -> "Request rejected successfully"
            StaffBookRequestStatus.PENDING -> rootObject?.stringValue("message", "status_message")
                ?: "Request updated successfully"
        }
        val requestElement = extractRequestElement(root)
        val parsedRequest = requestElement.asObjectOrNull()?.takeIf { it.entrySet().isNotEmpty() }?.let {
            parseRequest(requestElement, fallbackId = original.id)
        }

        return StaffBookRequestActionResult(
            message = message,
            request = parsedRequest ?: original.copy(status = fallbackStatus)
        )
    }

    private fun parseStatus(rawStatus: String?): StaffBookRequestStatus {
        return when (rawStatus?.trim()?.lowercase(Locale.US)) {
            "approved", "accepted", "approve" -> StaffBookRequestStatus.APPROVED
            "rejected", "declined", "denied", "reject" -> StaffBookRequestStatus.REJECTED
            else -> StaffBookRequestStatus.PENDING
        }
    }

    private fun normalizeAssetUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
        if (value.startsWith("http", ignoreCase = true)) {
            return value
                .replace("http://127.0.0.1:8000", apiRoot)
                .replace("http://localhost:8000", apiRoot)
                .replace("https://127.0.0.1:8000", apiRoot)
                .replace("https://localhost:8000", apiRoot)
        }
        val normalized = value.trimStart('/')
        val path = if (normalized.startsWith("storage/", ignoreCase = true)) normalized else "storage/$normalized"
        return "$apiRoot/$path"
    }

    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "-"
        val parsed = parseDate(rawDate) ?: return rawDate.take(16)
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(parsed)
    }

    private fun parseDateMillis(rawDate: String?): Long {
        return parseDate(rawDate)?.time ?: 0L
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

    private suspend fun <T> handleError(response: Response<*>): NetworkResult<T> {
        val message = parseErrorMessage(response)
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(
                "You are not authorized to manage book requests.",
                response.code()
            )
            Constants.HTTP_NOT_FOUND -> NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code())
            else -> NetworkResult.Error(message, response.code())
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) {
            return response.message().ifBlank { Constants.ERROR_UNKNOWN }
        }
        val parsed = runCatching { gson.fromJson(rawError, ErrorResponse::class.java) }.getOrNull()
        val firstValidationError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(160)
    }

    private fun JsonElement?.dataElement(): JsonElement? {
        val objectValue = this.asObjectOrNull() ?: return null
        return objectValue.get("data")
    }

    private fun JsonElement?.dataObject(): JsonObject? {
        return dataElement().asObjectOrNull()
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
                runCatching { value.asString }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }

    private fun JsonObject.intValue(vararg keys: String, fallback: Int = 0): Int {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asInt }.getOrNull()
            }
        } ?: fallback
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
