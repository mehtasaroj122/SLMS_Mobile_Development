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
import com.saroj.lmsmobile.ui.student.model.FineStatus
import com.saroj.lmsmobile.ui.student.model.MyBookStatus
import com.saroj.lmsmobile.ui.student.model.MyBookUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksSummaryUiModel
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentMyBooksRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getSummary(): Flow<NetworkResult<MyBooksSummaryUiModel>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_SUMMARY, MyBooksSummaryUiModel::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getStudentMyBooksSummary()
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

    fun getCurrentBooks(): Flow<NetworkResult<List<MyBookUiModel>>> = loadBooks(
        defaultStatus = MyBookStatus.ISSUED,
        request = { apiService.getStudentMyBooksCurrent() }
    )

    fun getHistoryBooks(): Flow<NetworkResult<List<MyBookUiModel>>> = loadBooks(
        defaultStatus = MyBookStatus.RETURNED,
        request = { apiService.getStudentMyBooksHistory() }
    )

    fun getDueSoonBooks(): Flow<NetworkResult<List<MyBookUiModel>>> = loadBooks(
        defaultStatus = MyBookStatus.DUE_SOON,
        request = { apiService.getStudentMyBooksDueSoon() }
    )

    private fun loadBooks(
        defaultStatus: MyBookStatus,
        request: suspend () -> Response<JsonElement>
    ): Flow<NetworkResult<List<MyBookUiModel>>> = flow {
        val cacheKey = "student:my-books:${defaultStatus.name.lowercase(Locale.US)}"
        val listType = object : TypeToken<List<MyBookUiModel>>() {}.type
        val cached = LocalCacheProvider.cache?.read<List<MyBookUiModel>>(cacheKey, listType)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = request()
        if (response.isSuccessful) {
            val books = extractBookElements(response.body()).mapIndexed { index, element ->
                parseBook(element, defaultStatus, index)
            }
            LocalCacheProvider.cache?.write(cacheKey, books)
            emit(NetworkResult.Success(books))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    private fun parseSummary(root: JsonElement?): MyBooksSummaryUiModel {
        val rootObject = root.asObjectOrNull()
        val dataObject = root.dataObject()
        
        // Try to find the best source object for stats
        // We look for "summary", then "stats", then try to see if "stats" is INSIDE "summary"
        val summaryObj = dataObject?.objectValue("summary") ?: rootObject?.objectValue("summary")
        val statsObj = dataObject?.objectValue("stats") ?: rootObject?.objectValue("stats")
        
        val source = summaryObj?.objectValue("stats") 
            ?: summaryObj 
            ?: statsObj 
            ?: dataObject 
            ?: rootObject 
            ?: JsonObject()
            
        // Prioritize explicit "pending" keys to avoid picking up "paid" fines
        var pendingFineValue = source.doubleValue(
            "pending_fine", 
            "pending_fines", 
            "pendingFine", 
            "total_pending_fine",
            "unpaid_fine",
            "unpaid_fines",
            "fine_pending",
            "pending_amount",
            "amount_due"
        )
        
        // If no explicit pending fine is found, try to calculate it or fall back to generic fine
        if (pendingFineValue <= 0.0) {
            val total = source.doubleValue("fine", "total_fine", "fine_amount", "amount", "total_amount")
            val paid = source.doubleValue("paid_fine", "paid_fines", "total_paid_fine", "amount_paid", "paid_amount")
            
            pendingFineValue = if (total > 0 && paid > 0) {
                (total - paid).coerceAtLeast(0.0)
            } else {
                total
            }
        }
        
        return MyBooksSummaryUiModel(
            totalIssued = source.intValue(
                "total_issued", 
                "issued_books", 
                "total_books", 
                "total_books_issued", 
                "total_issued_books",
                "total_issues",
                "all_issued",
                "totalIssued", 
                "total"
            ),
            currentlyBorrowed = source.intValue(
                "currently_borrowed",
                "current_borrowed",
                "borrowed_books",
                "current_books",
                "currently_borrowed_books",
                "currently_issued_books",
                "current_issued",
                "issued_current",
                "active_issues",
                "active_borrowed",
                "borrowed",
                "current_issues",
                "issued",
                "active",
                "currentlyBorrowed"
            ),
            overdueBooks = source.intValue("overdue_books", "overdue_issues", "overdueBooks", "overdue"),
            pendingFine = formatCurrency(pendingFineValue),
            pendingFineValue = pendingFineValue
        )
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

    private fun parseBook(element: JsonElement, defaultStatus: MyBookStatus, index: Int): MyBookUiModel {
        val issue = element.asObjectOrNull() ?: JsonObject()
        val pivot = issue.objectValue("pivot")
        val book = issue.objectValue("book") ?: issue.objectValue("book_data") ?: pivot?.objectValue("book") ?: JsonObject()
        val bookCopy = issue.objectValue("book_copy", "bookCopy", "copy", "copy_data") ?: pivot?.objectValue("book_copy") ?: JsonObject()
        val nestedCopyBook = bookCopy.objectValue("book", "book_data") ?: JsonObject()
        val fine = issue.objectValue("fine")
            ?: issue.objectValue("fine_data")
            ?: issue.arrayValue("fines")?.firstOrNull()?.asObjectOrNull()
            ?: pivot?.objectValue("fine")

        val rawIssueDate = issue.stringValue("issue_date", "issued_at", "issueDate")
        val rawDueDate = issue.stringValue("due_date", "dueDate")
        val rawReturnDate = issue.stringValue("return_date", "returned_at", "returnDate")
        
        // Comprehensive check for fine amount in multiple possible locations and keys
        val fineAmountValue = fine?.doubleValue("amount", "fine_amount", "value", "total", "fine", "fine_amt", "pending", "unpaid")
            ?: issue.doubleValue("fine_amount", "fineAmount", "pending_fine", "fine", "total_fine", "fine_amt", "amount", "penalty", "late_fee", "current_fine")
            ?: pivot?.doubleValue("fine_amount", "fine", "amount", "pending_fine")
            ?: 0.0
        
        val rawStatus = issue.stringValue("status", "issue_status")
        var status = parseStatus(rawStatus, defaultStatus)

        // Manual overdue check: if it's not returned and due date has passed, it's OVERDUE
        if (status != MyBookStatus.RETURNED) {
            val dueDateMillis = parseDateMillis(rawDueDate)
            if (dueDateMillis > 0 && dueDateMillis < System.currentTimeMillis()) {
                status = MyBookStatus.OVERDUE
            }
        }

        return MyBookUiModel(
            issueId = issue.intValue("id", "issue_id", fallback = fallbackIssueId(defaultStatus, index)),
            title = book.stringValue("title", "book_title")
                ?: issue.stringValue("title", "book_title")
                ?: "Untitled Book",
            author = book.stringValue("author")
                ?: issue.stringValue("author", "book_author")
                ?: "Unknown author",
            isbn = resolveIsbn(book, bookCopy, nestedCopyBook, issue),
            issueDate = formatDate(rawIssueDate),
            dueDate = formatDate(rawDueDate),
            returnDate = rawReturnDate?.let { formatDate(it) },
            status = status,
            fineAmount = formatFineAmount(fineAmountValue),
            fineStatus = parseFineStatus(
                rawStatus = fine?.stringValue(
                    "status",
                    "fine_status",
                    "fineStatus",
                    "payment_status",
                    "paymentStatus"
                ) ?: issue.stringValue(
                    "fine_status",
                    "fineStatus",
                    "payment_status",
                    "paymentStatus",
                    "fine_payment_status",
                    "finePaymentStatus"
                ),
                amount = fineAmountValue,
                fine = fine,
                issue = issue
            ),
            coverImageUrl = book.stringValue(
                "cover_image",
                "cover_image_url",
                "cover_url",
                "image_url",
                "cover",
                "thumbnail_url"
            ) ?: issue.stringValue("cover_image", "cover_image_url", "cover_url"),
            issueDateSort = parseDateMillis(rawIssueDate),
            dueDateSort = parseDateMillis(rawDueDate),
            fineAmountValue = fineAmountValue
        )
    }

    private fun resolveIsbn(
        book: JsonObject,
        bookCopy: JsonObject,
        nestedCopyBook: JsonObject,
        issue: JsonObject
    ): String {
        val isbnKeys = arrayOf(
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
        )

        return book.stringValue(*isbnKeys)
            ?: bookCopy.stringValue(*isbnKeys)
            ?: nestedCopyBook.stringValue(*isbnKeys)
            ?: issue.stringValue(*isbnKeys)
            ?: "-"
    }

    private fun parseStatus(rawStatus: String?, defaultStatus: MyBookStatus): MyBookStatus {
        return when (rawStatus?.trim()?.lowercase(Locale.US)) {
            "issued", "borrowed", "current", "active" -> defaultStatus.takeUnless { it == MyBookStatus.DUE_SOON }
                ?: MyBookStatus.DUE_SOON
            "due_soon", "due soon" -> MyBookStatus.DUE_SOON
            "overdue" -> MyBookStatus.OVERDUE
            "returned", "return" -> MyBookStatus.RETURNED
            else -> defaultStatus
        }
    }

    private fun fallbackIssueId(defaultStatus: MyBookStatus, index: Int): Int {
        val offset = when (defaultStatus) {
            MyBookStatus.ISSUED -> 100_000
            MyBookStatus.DUE_SOON -> 200_000
            MyBookStatus.OVERDUE -> 300_000
            MyBookStatus.RETURNED -> 400_000
        }
        return offset + index
    }

    private fun parseFineStatus(
        rawStatus: String?,
        amount: Double,
        fine: JsonObject?,
        issue: JsonObject
    ): FineStatus {
        if (rawStatus?.trim()?.lowercase(Locale.US) == "waived") return FineStatus.WAIVED

        if (fine?.booleanValue("is_paid", "paid", "isPaid") == true ||
            issue.booleanValue("fine_paid", "is_fine_paid", "isFinePaid") == true ||
            !fine?.stringValue("paid_date", "paid_at", "paidDate", "paidAt").isNullOrBlank() ||
            !issue.stringValue("fine_paid_date", "paid_date", "paid_at", "finePaidDate").isNullOrBlank()
        ) {
            return FineStatus.PAID
        }

        return when (rawStatus?.trim()?.lowercase(Locale.US)) {
            "paid", "completed", "settled", "paid_fine", "paidfine" -> FineStatus.PAID
            "waived" -> FineStatus.WAIVED
            "pending", "unpaid", "due", "not_paid", "not paid", "pending_fine", "unpaid_fine" -> FineStatus.UNPAID
            "none", "no_fine", "no fine", "0", "false" -> FineStatus.NONE
            else -> if (amount > 0.0) FineStatus.UNPAID else FineStatus.NONE
        }
    }

    private fun formatFineAmount(value: Double): String {
        return if (value <= 0.0) "No Fine" else String.format(Locale.US, "₹%.2f", value)
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "₹%.2f", value)
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

        val firstValidationError = parsed?.errors
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()

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

    private fun JsonObject.arrayValue(vararg keys: String): List<JsonElement>? {
        return keys.firstNotNullOfOrNull { key ->
            val value = get(key)
            if (value?.isJsonArray == true) value.asJsonArray.toList() else null
        }
    }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString?.takeIf { value ->
                value.isNotBlank()
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

    private fun JsonObject.doubleValue(vararg keys: String): Double {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asJsonPrimitive?.let { value ->
                runCatching { 
                    if (value.isNumber) {
                        value.asDouble 
                    } else {
                        val str = value.asString.replace(Regex("[^0-9.]"), "")
                        str.toDoubleOrNull() ?: value.asDouble
                    }
                }.getOrNull()
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
        const val CACHE_KEY_SUMMARY = "student:my-books:summary"
    }
}
