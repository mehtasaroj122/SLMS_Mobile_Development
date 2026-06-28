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
import com.saroj.lmsmobile.ui.student.model.MyFineStatus
import com.saroj.lmsmobile.ui.student.model.MyFineUiModel
import com.saroj.lmsmobile.ui.student.model.MyFinesSummaryUiModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class StudentMyFinesRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getSummary(): Flow<NetworkResult<MyFinesSummaryUiModel>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_SUMMARY, MyFinesSummaryUiModel::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getStudentFinesSummary()
        if (response.isSuccessful) {
            val summary = parseSummary(response.body())
            LocalCacheProvider.cache?.write(CACHE_KEY_SUMMARY, summary)
            emit(NetworkResult.Success(summary))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun getAllFines(): Flow<NetworkResult<List<MyFineUiModel>>> = loadFines(cacheName = "all", includeOverdue = true) {
        apiService.getAuthenticatedStudentFines()
    }

    fun getPendingFines(): Flow<NetworkResult<List<MyFineUiModel>>> = loadFines(cacheName = "pending", includeOverdue = true) {
        apiService.getAuthenticatedStudentPendingFines()
    }

    fun getPaidFines(): Flow<NetworkResult<List<MyFineUiModel>>> = loadFines(cacheName = "paid") {
        apiService.getAuthenticatedStudentPaidFines()
    }

    fun getFineDetail(id: Int): Flow<NetworkResult<MyFineUiModel>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.getAuthenticatedStudentFineDetail(id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseFine(extractFineElement(response.body()), id)))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun buildSummaryFromFines(fines: List<MyFineUiModel>): MyFinesSummaryUiModel {
        val pending = fines.filter { it.status == MyFineStatus.PENDING }
        val paid = fines.filter { it.status == MyFineStatus.PAID }
        val waived = fines.filter { it.status == MyFineStatus.WAIVED }

        val outstandingAmountValue = pending.sumOf { it.amountValue }

        return MyFinesSummaryUiModel(
            outstandingAmount = formatCurrency(outstandingAmountValue),
            outstandingCount = pending.size,
            paidAmount = formatCompactCurrency(paid.sumOf { it.amountValue }),
            paidCount = paid.size,
            waivedAmount = formatCurrency(waived.sumOf { it.amountValue }),
            waivedCount = waived.size,
            overdueBooks = pending.count { it.daysOverdue.toIntOrNull()?.let { days -> days > 0 } == true },
            outstandingAmountValue = outstandingAmountValue,
            paidAmountValue = paid.sumOf { it.amountValue },
            waivedAmountValue = waived.sumOf { it.amountValue }
        )
    }

    private fun loadFines(
        cacheName: String,
        includeOverdue: Boolean = false,
        request: suspend () -> Response<JsonElement>
    ): Flow<NetworkResult<List<MyFineUiModel>>> = flow {
        val cacheKey = "student:fines:$cacheName"
        val listType = object : TypeToken<List<MyFineUiModel>>() {}.type
        val cached = LocalCacheProvider.cache?.read<List<MyFineUiModel>>(cacheKey, listType)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = request()
        if (response.isSuccessful) {
            val fines = extractFineElements(response.body()).mapIndexed { index, element ->
                parseFine(element, fallbackId = index + 1)
            }

            var result = fines
            if (includeOverdue) {
                // Fetch current books to catch overdue ones that might be missing from the fines list
                val booksResponse = apiService.getStudentMyBooksCurrent()
                if (booksResponse.isSuccessful) {
                    val overdueFromBooks = extractBookElements(booksResponse.body())
                        .mapIndexed { i, el -> parseBookAsFine(el, i) }
                        .filter { it.daysOverdue != "-" && it.status == MyFineStatus.PENDING }
                    
                    // Merge: keep all original fines, add overdue books ONLY if their issueId isn't already there
                    val existingIssueIds = fines.mapNotNull { it.issueId }.toSet()
                    val missingOverdue = overdueFromBooks.filter { it.issueId !in existingIssueIds }
                    result = fines + missingOverdue
                }
            }

            val sorted = result.sortedByDescending { it.dueDateSort }
            LocalCacheProvider.cache?.write(cacheKey, sorted)
            emit(NetworkResult.Success(sorted))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    private fun parseSummary(root: JsonElement?): MyFinesSummaryUiModel {
        val rootObject = root.asObjectOrNull()
        val dataObject = root.dataObject()
        val source = dataObject?.objectValue("summary", "stats")
            ?: rootObject?.objectValue("summary", "stats")
            ?: dataObject
            ?: rootObject
            ?: JsonObject()

        val outstandingAmount = source.doubleValue(
            "outstanding_amount",
            "pending_amount",
            "total_pending",
            "pending_fines",
            "outstanding"
        )
        val paidAmount = source.doubleValue("paid_amount", "total_paid", "paid_fines", "paid")
        val waivedAmount = source.doubleValue("waived_amount", "total_waived", "waived_fines", "waived")

        return MyFinesSummaryUiModel(
            outstandingAmount = formatCurrency(outstandingAmount),
            outstandingCount = source.intValue("outstanding_count", "pending_count", "pending_fines_count"),
            paidAmount = formatCompactCurrency(paidAmount),
            paidCount = source.intValue("paid_count", "paid_fines_count"),
            waivedAmount = formatCurrency(waivedAmount),
            waivedCount = source.intValue("waived_count", "waived_fines_count"),
            overdueBooks = source.intValue("overdue_books", "overdue_books_count", "overdue_count"),
            outstandingAmountValue = outstandingAmount,
            paidAmountValue = paidAmount,
            waivedAmountValue = waivedAmount
        )
    }

    private fun extractFineElements(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()

        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            val dataObject = data.asJsonObject
            listOf("data", "fines", "items", "pending", "paid", "waived").forEach { key ->
                val nested = dataObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }

        val rootObject = root.asObjectOrNull() ?: return emptyList()
        listOf("fines", "items", "pending", "paid", "waived").forEach { key ->
            val nested = rootObject.get(key)
            if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
        }
        return emptyList()
    }

    private fun extractFineElement(root: JsonElement?, fallbackId: Int = 0): JsonElement {
        if (root == null || root.isJsonNull) {
            return JsonObject().apply { addProperty("id", fallbackId) }
        }

        if (root.isJsonObject) {
            val rootObject = root.asJsonObject
            val data = rootObject.get("data")
            if (data?.isJsonObject == true) {
                val dataObject = data.asJsonObject
                listOf("fine", "item").forEach { key ->
                    val nested = dataObject.get(key)
                    if (nested?.isJsonObject == true) return nested
                }
                return data
            }
            listOf("fine", "item").forEach { key ->
                val nested = rootObject.get(key)
                if (nested?.isJsonObject == true) return nested
            }
        }
        return root
    }

    private fun parseFine(element: JsonElement, fallbackId: Int): MyFineUiModel {
        val fine = element.asObjectOrNull() ?: JsonObject()
        val issue = fine.objectValue("issue", "book_issue", "borrow", "borrowing") ?: JsonObject()
        val book = fine.objectValue("book", "book_data")
            ?: issue.objectValue("book", "book_data")
            ?: issue.objectValue("book_copy", "copy")?.objectValue("book", "book_data")
            ?: JsonObject()
        val amountValue = fine.doubleValue("amount", "fine_amount", "total", "fine")
        val rawDueDate = fine.stringValue("due_date", "dueDate")
            ?: issue.stringValue("due_date", "dueDate")
            ?: fine.stringValue("fine_date", "created_at")
        val rawStatus = fine.stringValue("status", "fine_status", "payment_status")
        val status = parseStatus(rawStatus, fine)
        val daysOverdue = resolveDaysOverdue(fine, issue, rawDueDate)
        val rawReason = fine.stringValue("reason", "fine_reason", "type", "remarks", "description")

        // Use a unique ID: use fine ID if exists, otherwise a negative issue ID for virtual fines
        val stableId = fine.intValueNullable("id", "fine_id") 
            ?: issue.intValueNullable("id", "issue_id")?.let { -it } 
            ?: fallbackId

        return MyFineUiModel(
            id = stableId,
            bookTitle = book.stringValue("title", "book_title")
                ?: fine.stringValue("book_title", "title")
                ?: issue.stringValue("book_title", "title")
                ?: "Untitled Book",
            reason = normalizeReason(rawReason, daysOverdue),
            author = book.stringValue("author", "book_author")
                ?: fine.stringValue("author", "book_author")
                ?: issue.stringValue("author", "book_author")
                ?: "",
            isbn = book.stringValue(
                "isbn",
                "book_isbn",
                "isbn_no",
                "isbn_number",
                "isbn13",
                "isbn_13",
                "accession_no",
                "accessionNo",
                "accession_number",
                "accessionNumber",
                "barcode"
            ) ?: fine.stringValue("isbn", "book_isbn", "accession_no", "barcode")
                ?: issue.stringValue("isbn", "book_isbn", "accession_no", "barcode")
                ?: "",
            publisher = book.stringValue("publisher", "book_publisher", "publisher_name")
                ?: fine.stringValue("publisher", "book_publisher")
                ?: issue.stringValue("publisher", "book_publisher")
                ?: "",
            coverImageUrl = book.stringValue(
                "cover_image",
                "cover_image_url",
                "cover_url",
                "image_url",
                "thumbnail_url",
                "cover"
            ) ?: fine.stringValue("cover_image", "cover_image_url", "cover_url", "image_url")
                ?: issue.stringValue("cover_image", "cover_image_url", "cover_url", "image_url"),
            dueDate = formatDate(rawDueDate),
            daysOverdue = daysOverdue,
            amount = formatCurrency(amountValue),
            status = status,
            amountValue = amountValue,
            dueDateSort = parseDateMillis(rawDueDate),
            issueId = issue.intValueNullable("id", "issue_id")
        )
    }

    private fun normalizeReason(rawReason: String?, daysOverdue: String): String {
        val hasOverdueDays = daysOverdue.toIntOrNull()?.let { it > 0 } == true
        val fallback = if (hasOverdueDays) "Overdue" else "Fair Condition"
        val raw = rawReason?.trim()?.takeIf { it.isNotBlank() } ?: return fallback

        val withoutInvalidOverdue = if (!hasOverdueDays) {
            raw.replace(Regex("\\s*(\\+|,|&|and)?\\s*overdue\\s+fine\\s*", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\s*(\\+|,|&|and)?\\s*overdue\\s+penalty\\s*", RegexOption.IGNORE_CASE), " ")
                .trim()
        } else {
            raw
        }

        val cleaned = withoutInvalidOverdue
            .replace(Regex("\\s+"), " ")
            .trim(' ', '+', ',', '&', '-')
            .ifBlank { fallback }
        val lower = cleaned.lowercase(Locale.US)

        return when {
            lower.contains("fair condition") -> "Fair Condition"
            lower.contains("late return") -> "Late return"
            lower == "overdue" || lower.contains("overdue fine") -> "Overdue"
            else -> cleaned.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
        }
    }

    private fun parseStatus(rawStatus: String?, fine: JsonObject): MyFineStatus {
        if (fine.booleanValue("is_paid", "paid", "isPaid") == true ||
            !fine.stringValue("paid_date", "paid_at", "paidDate", "paidAt").isNullOrBlank()
        ) {
            return MyFineStatus.PAID
        }

        return when (rawStatus?.trim()?.lowercase(Locale.US)) {
            "paid", "completed", "settled" -> MyFineStatus.PAID
            "waived", "cancelled", "canceled" -> MyFineStatus.WAIVED
            else -> MyFineStatus.PENDING
        }
    }

    private fun resolveDaysOverdue(fine: JsonObject, issue: JsonObject, rawDueDate: String?): String {
        val explicitDays = fine.intValueNullable(
            "days_overdue",
            "overdue_days",
            "daysOverdue",
            "days_late"
        ) ?: issue.intValueNullable("days_overdue", "overdue_days", "daysOverdue", "days_late")

        explicitDays?.let { return if (it <= 0) "-" else it.toString() }

        val dueDate = parseDate(rawDueDate) ?: return "-"
        val diff = System.currentTimeMillis() - dueDate.time
        if (diff <= 0) return "-"
        val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
        return if (days <= 0) "-" else days.toString()
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "₹%.2f", value)
    }

    private fun formatCompactCurrency(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "₹%.0f", value)
        } else {
            formatCurrency(value)
        }
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

    private fun parseBookAsFine(element: JsonElement, index: Int): MyFineUiModel {
        val issue = element.asObjectOrNull() ?: JsonObject()
        val pivot = issue.objectValue("pivot")
        val book = issue.objectValue("book") ?: issue.objectValue("book_data") ?: pivot?.objectValue("book") ?: JsonObject()
        
        // Extract fine object if it exists
        val fine = issue.objectValue("fine")
            ?: issue.objectValue("fine_data")
            ?: issue.arrayValue("fines")?.firstOrNull()?.asObjectOrNull()
            ?: pivot?.objectValue("fine")

        val rawDueDate = issue.stringValue("due_date", "dueDate")
        val dueDateSort = parseDateMillis(rawDueDate)
        val daysOverdue = resolveDaysOverdue(JsonObject(), issue, rawDueDate)
        val isOverdue = daysOverdue != "-" && (daysOverdue.toIntOrNull() ?: 0) > 0

        // Comprehensive check for fine amount exactly like MyBooksRepository
        val fineAmount = fine?.doubleValue("amount", "fine_amount", "value", "total", "fine", "fine_amt", "pending", "unpaid")
            ?: issue.doubleValue("fine_amount", "fineAmount", "pending_fine", "fine", "total_fine", "fine_amt", "amount", "penalty", "late_fee", "current_fine")
            ?: pivot?.doubleValue("fine_amount", "fine", "amount", "pending_fine")
            ?: 0.0

        return MyFineUiModel(
            id = -1000 - (issue.intValue("id", "issue_id", fallback = index + 1)),
            bookTitle = book.stringValue("title") ?: "Untitled Book",
            reason = if (isOverdue) "Overdue" else "Current Issue",
            dueDate = formatDate(rawDueDate),
            daysOverdue = daysOverdue,
            amount = formatCurrency(fineAmount),
            status = MyFineStatus.PENDING,
            author = book.stringValue("author") ?: "",
            isbn = book.stringValue("isbn") ?: "",
            coverImageUrl = book.stringValue("cover_image", "cover_url"),
            amountValue = fineAmount,
            dueDateSort = dueDateSort,
            issueId = issue.intValueNullable("id", "issue_id")
        )
    }

    private fun JsonObject.arrayValue(vararg keys: String): List<JsonElement>? {
        return keys.firstNotNullOfOrNull { key ->
            val value = get(key)
            if (value?.isJsonArray == true) value.asJsonArray.toList() else null
        }
    }

    private fun extractBookElements(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()

        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            val dataObject = data.asJsonObject
            listOf("data", "books", "items", "current", "history", "due_soon").forEach { key ->
                val nested = dataObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }

        val rootObject = root.asObjectOrNull() ?: return emptyList()
        listOf("books", "items", "current", "history", "due_soon").forEach { key ->
            val nested = rootObject.get(key)
            if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
        }
        return emptyList()
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
        return intValueNullable(*keys) ?: fallback
    }

    private fun JsonObject.intValueNullable(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asInt }.getOrNull()
            }
        }
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

    private companion object {
        const val CACHE_KEY_SUMMARY = "student:fines:summary"
    }
}
