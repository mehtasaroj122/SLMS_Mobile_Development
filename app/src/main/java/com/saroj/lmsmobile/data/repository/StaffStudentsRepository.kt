package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.fine.WaiveFineRequest
import com.saroj.lmsmobile.data.models.staffdashboard.RejectBookRequestBody
import com.saroj.lmsmobile.data.models.staffstudents.RequestBookInfo
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentActionResponse
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentDetailData
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentDetailResponse
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentItem
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentsResponse
import com.saroj.lmsmobile.data.models.staffstudents.StudentBookRequestItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentBookRequestsResponse
import com.saroj.lmsmobile.data.models.staffstudents.StudentFineItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentFineSummary
import com.saroj.lmsmobile.data.models.staffstudents.StudentFinesData
import com.saroj.lmsmobile.data.models.staffstudents.StudentFinesResponse
import com.saroj.lmsmobile.data.models.staffstudents.StudentIssuedBookItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentIssuedBooksResponse
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class StaffStudentsRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getStudents(status: String, search: String): Flow<NetworkResult<StaffStudentsResponse>> = flow {
        val query = search.trim().takeIf { it.isNotBlank() }
        val cacheKey = "staff:students:list:status=$status:query=${query.orEmpty()}"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StaffStudentsResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        val response = apiService.getStaffStudents(
            status = status,
            search = query,
            query = query
        )
        if (response.isSuccessful) {
            val students = extractArray(response.body()).mapIndexed { index, element ->
                parseStudent(element, fallbackId = index + 1)
            }.let { list ->
                if (status == "all") list else list.filter { it.normalizedStatus == status }
            }
            val result = StaffStudentsResponse(message = messageOf(response.body()), data = students)
            LocalCacheProvider.cache?.write(cacheKey, result)
            emit(NetworkResult.Success(result))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getStudentDetails(studentId: Int): Flow<NetworkResult<StaffStudentDetailResponse>> = flow {
        val cacheKey = "staff:students:detail:$studentId"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StaffStudentDetailResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        val response = apiService.getStaffIssueStudentDetail(studentId)
        if (response.isSuccessful) {
            val root = response.body()
            val data = root.dataElement()
            val studentElement = data.asObjectOrNull()?.get("student") ?: data ?: root
            val student = parseStudent(studentElement, fallbackId = studentId)
            val result = StaffStudentDetailResponse(
                message = messageOf(root),
                data = StaffStudentDetailData(student)
            )
            LocalCacheProvider.cache?.write(cacheKey, result)
            emit(NetworkResult.Success(result))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getIssuedBooks(studentId: Int): Flow<NetworkResult<StudentIssuedBooksResponse>> = flow {
        val cacheKey = "staff:students:issued:$studentId"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StudentIssuedBooksResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        val response = apiService.getStaffStudentIssuedBooks(studentId)
        if (response.isSuccessful) {
            val books = extractArray(response.body()).mapIndexed { index, element ->
                parseIssuedBook(element, fallbackId = index + 1)
            }
            val result = StudentIssuedBooksResponse(message = messageOf(response.body()), data = books)
            LocalCacheProvider.cache?.write(cacheKey, result)
            emit(NetworkResult.Success(result))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getStudentFines(studentId: Int): Flow<NetworkResult<StudentFinesResponse>> = flow {
        val cacheKey = "staff:students:fines:$studentId"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StudentFinesResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        val response = apiService.getStudentFineDetails(studentId)
        if (response.isSuccessful) {
            val body = response.body()
            val data = body?.data
            val student = data?.student?.let {
                StaffStudentItem(
                    id = it.displayId,
                    userId = it.userId,
                    name = it.name,
                    email = it.email,
                    studentId = it.studentId?.toString(),
                    rollNo = it.rollNo,
                    symbolNo = it.symbolNo,
                    studentCode = it.studentCode,
                    department = it.department,
                    photo = it.photo,
                    profilePhotoUrl = it.profilePhotoUrl
                )
            }
            val summary = data?.summary?.let {
                StudentFineSummary(
                    totalFine = it.totalFine,
                    pendingAmount = it.pendingAmount,
                    paidAmount = it.paidAmount,
                    waivedAmount = it.waivedAmount,
                    recordsCount = it.recordsCount
                )
            } ?: StudentFineSummary()
            val fines = data?.fines.orEmpty().map {
                StudentFineItem(
                    id = it.id,
                    fineId = it.fineId,
                    bookTitle = it.bookTitle,
                    author = it.author,
                    dueDate = it.dueDate,
                    returnDate = it.returnDate,
                    daysOverdue = it.daysOverdue,
                    daysLate = it.daysLate,
                    amount = it.amount,
                    status = it.status,
                    reason = it.reason,
                    fineType = it.fineType,
                    remarks = it.remarks,
                    waiveReason = it.waiveReason,
                    paidAt = it.paidAt,
                    paidDate = it.paidDate,
                    waivedAt = it.waivedAt,
                    coverImageUrl = it.displayCover
                )
            }
            val result = StudentFinesResponse(message = body?.message, data = StudentFinesData(student, summary, fines))
            LocalCacheProvider.cache?.write(cacheKey, result)
            emit(NetworkResult.Success(result))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getStudentBookRequests(studentId: Int): Flow<NetworkResult<StudentBookRequestsResponse>> = flow {
        val cacheKey = "staff:students:requests:$studentId"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StudentBookRequestsResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        val response = apiService.getStaffStudentBookRequests(studentId)
        if (response.isSuccessful) {
            val requests = extractArray(response.body()).mapIndexed { index, element ->
                parseBookRequest(element, fallbackId = index + 1)
            }
            val result = StudentBookRequestsResponse(message = messageOf(response.body()), data = requests)
            LocalCacheProvider.cache?.write(cacheKey, result)
            emit(NetworkResult.Success(result))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun markFinePaid(fineId: Int): Flow<NetworkResult<StaffStudentActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.markFinePaid(fineId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(StaffStudentActionResponse(message = response.body()?.message ?: "Fine marked as paid successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun waiveFine(fineId: Int, reason: String): Flow<NetworkResult<StaffStudentActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.waiveFine(fineId, WaiveFineRequest(reason.trim()))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(StaffStudentActionResponse(message = response.body()?.message ?: "Fine waived successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun approveBookRequest(requestId: Int): Flow<NetworkResult<StaffStudentActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.approveStaffStudentBookRequest(requestId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: StaffStudentActionResponse(message = "Request accepted successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun rejectBookRequest(requestId: Int): Flow<NetworkResult<StaffStudentActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.rejectStaffStudentBookRequest(requestId, RejectBookRequestBody())
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: StaffStudentActionResponse(message = "Request rejected successfully")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private fun parseStudent(element: JsonElement?, fallbackId: Int): StaffStudentItem {
        val source = element.asObjectOrNull() ?: JsonObject()
        val user = source.objectValue("user") ?: JsonObject()
        return StaffStudentItem(
            id = source.intValue("id", "student_id_number", fallback = fallbackId),
            userId = source.intValueOrNull("user_id") ?: user.intValueOrNull("id"),
            name = source.stringValue("name", "full_name") ?: user.stringValue("name", "full_name"),
            email = source.stringValue("email") ?: user.stringValue("email"),
            phone = source.stringValue(
                "phone",
                "phone_number",
                "phone_no",
                "mobile",
                "mobile_number",
                "contact",
                "contact_number"
            ) ?: user.stringValue(
                "phone",
                "phone_number",
                "phone_no",
                "mobile",
                "mobile_number",
                "contact",
                "contact_number"
            ),
            studentId = source.stringValue("student_id", "student_code"),
            rollNo = source.stringValue("roll_no", "roll_number"),
            symbolNo = source.stringValue("symbol_no", "symbol_number"),
            studentCode = source.stringValue("student_code", "code"),
            department = source.stringValue("department", "department_name") ?: user.stringValue("department"),
            faculty = source.stringValue("faculty"),
            batch = source.stringValue("batch", "year"),
            status = source.stringValue("status", "account_status") ?: user.stringValue("status"),
            photo = source.stringValue("photo", "photo_url"),
            profilePhoto = source.stringValue("profile_photo") ?: user.stringValue("profile_photo"),
            profilePhotoUrl = source.stringValue("profile_photo_url") ?: user.stringValue("profile_photo_url")
        )
    }

    private fun parseIssuedBook(element: JsonElement?, fallbackId: Int): StudentIssuedBookItem {
        val issue = element.asObjectOrNull() ?: JsonObject()
        val book = issue.objectValue("book", "book_data") ?: JsonObject()
        val fine = issue.objectValue("fine") ?: JsonObject()
        return StudentIssuedBookItem(
            issueId = issue.intValue("issue_id", "id", fallback = fallbackId),
            bookId = issue.intValueOrNull("book_id") ?: book.intValueOrNull("id"),
            title = book.stringValue("title", "book_title") ?: issue.stringValue("title", "book_title"),
            author = book.stringValue("author", "book_author") ?: issue.stringValue("author", "book_author"),
            isbn = book.stringValue("isbn") ?: issue.stringValue("isbn"),
            accessionNo = book.stringValue("accession_no", "accession_number") ?: issue.stringValue("accession_no", "accession_number"),
            issueDate = issue.stringValue("issue_date"),
            issuedDate = issue.stringValue("issued_date"),
            dueDate = issue.stringValue("due_date"),
            returnDate = issue.stringValue("return_date", "returned_at"),
            status = issue.stringValue("status", "issue_status"),
            fineAmount = issue.doubleValueOrNull("fine_amount") ?: fine.doubleValueOrNull("amount") ?: 0.0,
            coverImageUrl = coverUrl(book, issue)
        )
    }

    private fun parseBookRequest(element: JsonElement?, fallbackId: Int): StudentBookRequestItem {
        val request = element.asObjectOrNull() ?: JsonObject()
        val book = request.objectValue("book", "book_data") ?: JsonObject()
        val processedBy = request.objectValue("processed_by_user", "processor", "processed_by") ?: JsonObject()
        return StudentBookRequestItem(
            id = request.intValue("id", "request_id", "book_request_id", fallback = fallbackId),
            requestId = request.intValueOrNull("request_id"),
            book = RequestBookInfo(
                id = book.intValueOrNull("id"),
                title = book.stringValue("title", "book_title"),
                author = book.stringValue("author", "book_author"),
                coverImageUrl = coverUrl(book, request)
            ),
            bookTitle = request.stringValue("book_title", "title"),
            author = request.stringValue("author", "book_author"),
            requestDate = request.stringValue("request_date"),
            requestedAt = request.stringValue("requested_at"),
            createdAt = request.stringValue("created_at"),
            status = request.stringValue("status", "request_status"),
            message = request.stringValue("message", "remarks", "reason"),
            processedBy = processedBy.stringValue("name") ?: request.stringValue("processed_by", "processed_by_name"),
            processedAt = request.stringValue("processed_at", "processed_date"),
            coverImageUrl = coverUrl(book, request)
        )
    }

    private fun coverUrl(book: JsonObject, fallback: JsonObject): String? =
        book.stringValue(
            "cover_image",
            "cover_image_url",
            "cover_url",
            "image_url",
            "thumbnail_url",
            "cover"
        ) ?: fallback.stringValue("cover_image", "cover_image_url", "cover_url", "image_url", "thumbnail_url", "cover")

    private fun extractArray(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()
        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            listOf("data", "students", "issues", "requests", "book_requests", "items").forEach { key ->
                val nested = data.asJsonObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }
        root.asObjectOrNull()?.let { objectValue ->
            listOf("students", "issues", "requests", "book_requests", "items").forEach { key ->
                val nested = objectValue.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }
        return emptyList()
    }

    private suspend fun <T> handleError(response: Response<*>): NetworkResult<T> =
        when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(
                "You are not authorized to view student records.",
                response.code()
            )
            else -> NetworkResult.Error(parseErrorMessage(response), response.code())
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

    private fun messageOf(root: JsonElement?): String? = root.asObjectOrNull()?.stringValue("message")

    private fun JsonElement?.dataElement(): JsonElement? =
        this.asObjectOrNull()?.get("data")

    private fun JsonElement?.asObjectOrNull(): JsonObject? =
        if (this != null && !isJsonNull && isJsonObject) asJsonObject else null

    private fun JsonObject.objectValue(vararg keys: String): JsonObject? =
        keys.firstNotNullOfOrNull { key -> get(key).asObjectOrNull() }

    private fun JsonObject.stringValue(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let {
                runCatching { it.asString }.getOrNull()?.takeIf { value -> value.isNotBlank() }
            }
        }

    private fun JsonObject.intValue(vararg keys: String, fallback: Int = 0): Int =
        intValueOrNull(*keys) ?: fallback

    private fun JsonObject.intValueOrNull(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let {
                runCatching { it.asInt }.getOrNull()
            }
        }

    private fun JsonObject.doubleValueOrNull(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let {
                runCatching { it.asDouble }.getOrNull()
            }
        }
}
