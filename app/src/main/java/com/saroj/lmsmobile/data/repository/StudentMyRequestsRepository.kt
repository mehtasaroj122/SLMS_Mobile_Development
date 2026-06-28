package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.student.model.MyRequestActionResult
import com.saroj.lmsmobile.ui.student.model.MyRequestStatus
import com.saroj.lmsmobile.ui.student.model.MyRequestUiModel
import com.saroj.lmsmobile.ui.student.model.MyRequestsSummaryUiModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentMyRequestsRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getSummary(): Flow<NetworkResult<MyRequestsSummaryUiModel>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_SUMMARY, MyRequestsSummaryUiModel::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        val response = apiService.getStudentRequestsSummary()
        if (response.isSuccessful) {
            val summary = parseSummary(response.body())
            LocalCacheProvider.cache?.write(CACHE_KEY_SUMMARY, summary)
            emit(NetworkResult.Success(summary))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getRequests(): Flow<NetworkResult<List<MyRequestUiModel>>> = flow {
        val listType = object : TypeToken<List<MyRequestUiModel>>() {}.type
        val cached = LocalCacheProvider.cache?.read<List<MyRequestUiModel>>(CACHE_KEY_REQUESTS, listType)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        val response = apiService.getAuthenticatedStudentRequests()
        if (response.isSuccessful) {
            val requests = extractRequestElements(response.body()).mapIndexed { index, element ->
                parseRequest(element, fallbackId = index + 1)
            }
            val sorted = requests.sortedByDescending { it.requestDateSort }
            LocalCacheProvider.cache?.write(CACHE_KEY_REQUESTS, sorted)
            emit(NetworkResult.Success(sorted))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getRequestDetail(id: Int): Flow<NetworkResult<MyRequestUiModel>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getAuthenticatedStudentRequestDetail(id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseRequest(extractRequestElement(response.body()), fallbackId = id)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun cancelRequest(request: MyRequestUiModel): Flow<NetworkResult<MyRequestActionResult>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.cancelAuthenticatedStudentRequest(request.id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseCancelResult(response.body(), request)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun buildSummaryFromRequests(requests: List<MyRequestUiModel>): MyRequestsSummaryUiModel {
        return MyRequestsSummaryUiModel(
            totalRequests = requests.size,
            pendingRequests = requests.count { it.status == MyRequestStatus.PENDING },
            approvedRequests = requests.count { it.status == MyRequestStatus.APPROVED },
            rejectedRequests = requests.count { it.status == MyRequestStatus.REJECTED }
        )
    }

    private fun parseSummary(root: JsonElement?): MyRequestsSummaryUiModel {
        val rootObject = root.asObjectOrNull()
        val dataObject = root.dataObject()
        val source = dataObject?.objectValue("summary", "stats")
            ?: rootObject?.objectValue("summary", "stats")
            ?: dataObject
            ?: rootObject
            ?: JsonObject()

        return MyRequestsSummaryUiModel(
            totalRequests = source.intValue(
                "total_requests",
                "totalRequests",
                "total",
                "requests_count",
                "count"
            ),
            pendingRequests = source.intValue(
                "pending_requests",
                "pendingRequests",
                "pending",
                "pending_count"
            ),
            approvedRequests = source.intValue(
                "approved_requests",
                "approvedRequests",
                "approved",
                "approved_count"
            ),
            rejectedRequests = source.intValue(
                "rejected_requests",
                "rejectedRequests",
                "rejected",
                "rejected_count"
            )
        )
    }

    private fun extractRequestElements(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()

        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            val dataObject = data.asJsonObject
            listOf("data", "requests", "book_requests", "items").forEach { key ->
                val nested = dataObject.get(key)
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
            if (data?.isJsonObject == true) {
                val dataObject = data.asJsonObject
                listOf("request", "book_request", "item").forEach { key ->
                    val nested = dataObject.get(key)
                    if (nested?.isJsonObject == true) return nested
                }
                return data
            }
            listOf("request", "book_request", "item").forEach { key ->
                val nested = rootObject.get(key)
                if (nested?.isJsonObject == true) return nested
            }
        }
        return root
    }

    private fun parseRequest(element: JsonElement, fallbackId: Int): MyRequestUiModel {
        val request = element.asObjectOrNull() ?: JsonObject()
        val book = request.objectValue("book", "book_data")
            ?: request.objectValue("book_copy", "copy", "bookCopy")?.objectValue("book", "book_data")
            ?: JsonObject()
        val rawStatus = request.stringValue("status", "request_status", "state")
        val status = parseStatus(rawStatus)
        val rawDate = request.stringValue(
            "requested_at",
            "request_date",
            "requestDate",
            "created_at",
            "createdAt"
        )

        return MyRequestUiModel(
            id = request.intValue("id", "request_id", "book_request_id", fallback = fallbackId),
            bookTitle = book.stringValue("title", "book_title")
                ?: request.stringValue("book_title", "title")
                ?: "Untitled Book",
            author = book.stringValue("author", "book_author")
                ?: request.stringValue("author", "book_author")
                ?: "Unknown author",
            requestDate = formatDate(rawDate),
            status = status,
            processedBy = resolveProcessedBy(request, status),
            message = resolveMessage(request, status),
            coverImageUrl = normalizeCoverUrl(
                book.stringValue(
                    "cover_image",
                    "cover_image_url",
                    "cover_url",
                    "image_url",
                    "thumbnail_url",
                    "cover"
                ) ?: request.stringValue("cover_image", "cover_image_url", "cover_url", "image_url")
            ),
            requestDateSort = parseDateMillis(rawDate)
        )
    }

    private fun parseCancelResult(root: JsonElement?, original: MyRequestUiModel): MyRequestActionResult {
        val rootObject = root.asObjectOrNull()
        val message = rootObject?.stringValue("message", "status_message")
            ?: "Request cancelled successfully."
        val requestElement = extractRequestElement(root)
        val parsedRequest = requestElement.asObjectOrNull()?.takeIf { it.entrySet().isNotEmpty() }?.let {
            parseRequest(requestElement, fallbackId = original.id)
        }

        return MyRequestActionResult(
            message = message,
            request = parsedRequest ?: original.copy(
                status = MyRequestStatus.CANCELLED,
                processedBy = "You",
                message = "Request cancelled by you."
            )
        )
    }

    private fun resolveProcessedBy(request: JsonObject, status: MyRequestStatus): String {
        request.stringValue(
            "processed_by",
            "processed_by_name",
            "processedBy",
            "approved_by_name",
            "rejected_by_name",
            "cancelled_by_name",
            "canceled_by_name"
        )?.let { return it }

        val user = request.objectValue(
            "processor",
            "processed_by_user",
            "processedByUser",
            "librarian",
            "staff",
            "approved_by",
            "rejected_by",
            "cancelled_by",
            "canceled_by"
        )
        user?.stringValue("name", "full_name", "username")?.let { return it }

        return if (status == MyRequestStatus.PENDING) "N/A" else "-"
    }

    private fun resolveMessage(request: JsonObject, status: MyRequestStatus): String {
        request.stringValue(
            "message",
            "remarks",
            "remark",
            "reason",
            "note",
            "notes",
            "admin_message"
        )?.let { return it }

        return when (status) {
            MyRequestStatus.PENDING -> "Waiting for librarian approval."
            MyRequestStatus.APPROVED -> "Your request has been approved."
            MyRequestStatus.REJECTED -> "Your request has been rejected."
            MyRequestStatus.CANCELLED -> "Request cancelled by you."
        }
    }

    private fun parseStatus(rawStatus: String?): MyRequestStatus {
        return when (rawStatus?.trim()?.lowercase(Locale.US)) {
            "approved", "accepted", "approve" -> MyRequestStatus.APPROVED
            "rejected", "declined", "denied", "reject" -> MyRequestStatus.REJECTED
            "cancelled", "canceled", "cancel" -> MyRequestStatus.CANCELLED
            else -> MyRequestStatus.PENDING
        }
    }

    private fun normalizeCoverUrl(rawCoverUrl: String?): String? {
        val value = rawCoverUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val apiRoot = Constants.BASE_URL.removeSuffix("api/").trimEnd('/')
        if (value.startsWith("http", ignoreCase = true)) {
            return value
                .let { Constants.normalizeLaravelAssetUrl(it) ?: it }
        }

        val normalizedPath = value.trimStart('/')
        val storagePath = if (normalizedPath.startsWith("storage/", ignoreCase = true)) {
            normalizedPath
        } else {
            "storage/$normalizedPath"
        }
        return "$apiRoot/$storagePath"
    }

    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "-"
        val parsed = parseDate(rawDate) ?: return rawDate.take(10)
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
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            Constants.HTTP_NOT_FOUND -> NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code())
            else -> NetworkResult.Error(message, response.code())
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) {
            return response.message().ifBlank { Constants.ERROR_UNKNOWN }
        }

        val parsed = runCatching {
            gson.fromJson(rawError, ErrorResponse::class.java)
        }.getOrNull()
        val firstValidationError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(140)
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
        const val CACHE_KEY_SUMMARY = "student:requests:summary"
        const val CACHE_KEY_REQUESTS = "student:requests:list"
    }
}
