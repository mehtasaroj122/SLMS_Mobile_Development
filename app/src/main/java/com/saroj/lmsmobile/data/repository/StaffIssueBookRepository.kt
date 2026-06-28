package com.saroj.lmsmobile.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.issue.BookSearchResponse
import com.saroj.lmsmobile.data.models.issue.IssueBookItem
import com.saroj.lmsmobile.data.models.issue.IssueBooksData
import com.saroj.lmsmobile.data.models.issue.IssueBooksRequest
import com.saroj.lmsmobile.data.models.issue.IssueBooksResponse
import com.saroj.lmsmobile.data.models.issue.IssuePrivilegesData
import com.saroj.lmsmobile.data.models.issue.IssuePrivilegesResponse
import com.saroj.lmsmobile.data.models.issue.IssueSearchMeta
import com.saroj.lmsmobile.data.models.issue.IssueStudent
import com.saroj.lmsmobile.data.models.issue.StudentIssueContext
import com.saroj.lmsmobile.data.models.issue.StudentSearchResponse
import com.saroj.lmsmobile.data.models.student.Student
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class StaffIssueBookRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun searchStudents(
        query: String,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Flow<NetworkResult<StudentSearchResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.searchStaffIssueStudents(query, page = page, pageSize = pageSize)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseStudentSearch(response.body())))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(searchStudentsFallback(query, page, pageSize))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Student search failed", e)
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun getStudentIssueContext(student: IssueStudent): Flow<NetworkResult<StudentIssueContext>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val detailedStudent = loadStudentDetail(student)
        val activeIssueCount = loadActiveIssueCount(student.id)
        val enrichedStudent = detailedStudent.copy(
            currentIssued = detailedStudent.currentIssued ?: activeIssueCount
        )

        val privilegesResponse = apiService.getStaffStudentIssuePrivileges(student.id)
        when {
            privilegesResponse.isSuccessful -> {
                val parsed = parseIssueContext(privilegesResponse.body(), enrichedStudent, activeIssueCount)
                emit(NetworkResult.Success(parsed))
            }
            privilegesResponse.code() == Constants.HTTP_NOT_FOUND -> {
                emit(NetworkResult.Success(StudentIssueContext(enrichedStudent, fallbackPrivileges(enrichedStudent, activeIssueCount))))
            }
            else -> emit(handleErrorResponse(privilegesResponse))
        }
    }.catch { e ->
        Log.e(TAG, "Student issue context failed", e)
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun searchBooks(
        query: String,
        studentId: Int,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Flow<NetworkResult<BookSearchResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.searchStaffIssueBooks(
            query = query,
            studentId = studentId,
            page = page,
            pageSize = pageSize
        )
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseBookSearch(response.body()).withEnrichedCovers()))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(searchBooksFallback(query, page, pageSize))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Book search failed", e)
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun issueBooks(studentId: Int, bookIds: List<Int>): Flow<NetworkResult<IssueBooksResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.issueStaffBooks(IssueBooksRequest(studentId, bookIds))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseIssueResponse(response.body(), bookIds.size)))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(issueBooksOneByOne(studentId, bookIds))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Issue books failed", e)
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    private suspend fun issueBooksOneByOne(studentId: Int, bookIds: List<Int>): NetworkResult<IssueBooksResponse> {
        var issuedCount = 0
        for (bookId in bookIds) {
            val response = apiService.createIssue(com.saroj.lmsmobile.data.models.issue.IssueRequest(studentId, bookId))
            if (response.isSuccessful) {
                issuedCount++
            } else {
                return handleErrorResponse(response)
            }
        }
        return NetworkResult.Success(
            IssueBooksResponse(
                success = true,
                message = "Books issued successfully.",
                data = IssueBooksData(issuedCount = issuedCount)
            )
        )
    }

    private suspend fun loadStudentDetail(baseStudent: IssueStudent): IssueStudent {
        var response = apiService.getStaffIssueStudentDetail(baseStudent.id)
        if (!response.isSuccessful && response.code() == Constants.HTTP_NOT_FOUND) {
            response = apiService.getStudentDetailJson(baseStudent.id)
        }
        if (!response.isSuccessful) return baseStudent
        val parsed = parseStudentDetail(response.body()) ?: return baseStudent
        return baseStudent.mergeWith(parsed)
    }

    private suspend fun loadActiveIssueCount(studentId: Int): Int? {
        val response = apiService.getStudentIssues(studentId, page = 1, pageSize = 100)
        if (!response.isSuccessful) return null
        return response.body()?.data
            ?.count { issue ->
                issue.returnDate.isNullOrBlank() &&
                    !issue.status.equals("returned", ignoreCase = true)
            }
    }

    private suspend fun searchStudentsFallback(
        query: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<StudentSearchResponse> {
        val response = apiService.searchStudentsJson(query, page = page, pageSize = pageSize)
        return if (response.isSuccessful) {
            NetworkResult.Success(parseStudentSearch(response.body()))
        } else {
            handleErrorResponse(response)
        }
    }

    private suspend fun searchBooksFallback(
        query: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<BookSearchResponse> {
        val response = apiService.searchBooks(
            query = query,
            page = page,
            pageSize = pageSize
        )
        return if (response.isSuccessful) {
            val books = response.body()?.data.orEmpty().map { it.toIssueBookItem() }
            val pagination = response.body()?.meta
            NetworkResult.Success(
                BookSearchResponse(
                    data = books,
                    meta = IssueSearchMeta(
                        currentPage = pagination?.current_page ?: page,
                        lastPage = pagination?.last_page ?: page,
                        perPage = pagination?.per_page ?: pageSize,
                        total = pagination?.total ?: books.size
                    )
                ).withEnrichedCovers()
            )
        } else {
            handleErrorResponse(response)
        }
    }

    private suspend fun BookSearchResponse.withEnrichedCovers(): BookSearchResponse {
        val enriched = data.map { book ->
            if (!book.coverImageUrl.isNullOrBlank()) {
                book
            } else {
                loadBookCover(book.requestId)?.let { book.copy(coverImageUrl = it) } ?: book
            }
        }
        return copy(data = enriched)
    }

    private suspend fun loadBookCover(bookId: Int): String? {
        val response = apiService.getBookDetailJson(bookId)
        if (!response.isSuccessful) return null
        val root = response.body().asObjectOrNull()
        val data = root?.get("data")?.asObjectOrNull()
            ?: root?.get("book")?.asObjectOrNull()
            ?: root
        return data?.string(
            "cover_image",
            "cover_image_url",
            "cover_url",
            "image_url",
            "thumbnail_url",
            "cover"
        )
    }

    private fun parseStudentSearch(body: JsonElement?): StudentSearchResponse {
        val root = body.asObjectOrNull()
        val array = findListArray(root, "students", "results", "items")
        val students = array?.mapNotNull { element ->
            parseIssueStudent(element)
        }.orEmpty()
        return StudentSearchResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = students,
            meta = parseSearchMeta(root, students.size)
        )
    }

    private fun parseStudentDetail(body: JsonElement?): IssueStudent? {
        val root = body.asObjectOrNull() ?: return null
        val data = root.get("data")?.asObjectOrNull()
        val studentObject = data?.get("student")?.asObjectOrNull()
            ?: root.get("student")?.asObjectOrNull()
            ?: data
            ?: root
        return parseIssueStudent(studentObject)
    }

    private fun parseIssueContext(
        body: JsonElement?,
        fallbackStudent: IssueStudent,
        activeIssueCount: Int?
    ): StudentIssueContext {
        val root = body.asObjectOrNull()
        val data = root?.get("data")?.asObjectOrNull() ?: root
        val responseStudent = parseIssueStudent(data?.get("student"))
        val mergedStudent = if (responseStudent != null) fallbackStudent.mergeWith(responseStudent) else fallbackStudent
        val privileges = parsePrivilegesObject(
            data?.get("privileges")?.asObjectOrNull()
                ?: root?.get("privileges")?.asObjectOrNull()
                ?: data,
            mergedStudent,
            activeIssueCount
        )
        return StudentIssueContext(mergedStudent, privileges)
    }

    private fun parsePrivileges(body: JsonElement?): IssuePrivilegesResponse {
        val root = body.asObjectOrNull()
        val dataObject = root?.get("data")?.asObjectOrNull()
            ?: root?.get("privileges")?.asObjectOrNull()
            ?: root
        val privilegesObject = dataObject?.get("privileges")?.asObjectOrNull() ?: dataObject
        val student = dataObject?.get("student")?.let(::parseIssueStudent)
        val privileges = parsePrivilegesObject(privilegesObject, student, student?.currentIssued)
        return IssuePrivilegesResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = privileges
        )
    }

    private fun parsePrivilegesObject(
        source: JsonObject?,
        student: IssueStudent?,
        activeIssueCount: Int?
    ): IssuePrivilegesData {
        val maxBooks = source?.int("max_books", "maxBooks", "max_allowed_books", "issue_limit")
            ?: student?.maxBooks
        val alreadyIssued = source?.int("already_issued", "alreadyIssued", "current_issued", "currently_issued")
            ?: student?.currentIssued
            ?: activeIssueCount
        val canIssue = source?.int("can_issue", "canIssue", "remaining", "remaining_books")
            ?: maxBooks?.let { (it - (alreadyIssued ?: 0)).coerceAtLeast(0) }
        val accountStatus = source?.string("account_status") ?: student?.status
        val borrowingStatus = source?.string("borrowing_status", "borrow_status")
        val explicitAllowed = source?.bool("allowed", "can_issue_books", "eligible")
        val blockedByStatus = listOf(accountStatus, borrowingStatus).any { status ->
            status.equals("inactive", ignoreCase = true) ||
                status.equals("suspended", ignoreCase = true) ||
                status.equals("blocked", ignoreCase = true) ||
                status.equals("disabled", ignoreCase = true)
        }
        val allowed = explicitAllowed ?: (!blockedByStatus && (canIssue ?: 1) > 0)

        return IssuePrivilegesData(
            allowed = allowed,
            reason = source?.string("reason", "message"),
            maxBooks = maxBooks,
            alreadyIssued = alreadyIssued,
            canIssue = canIssue,
            durationDays = source?.int("duration_days", "durationDays", "duration", "issue_duration", "issue_duration_days"),
            fineRate = source?.string("fine_rate", "fineRate", "fine_per_day", "per_day_fine"),
            issueDate = source?.string("issue_date", "issueDate"),
            dueDate = source?.string("due_date", "dueDate"),
            settingsAvailable = source != null
        )
    }

    private fun parseBookSearch(body: JsonElement?): BookSearchResponse {
        val root = body.asObjectOrNull()
        val array = findListArray(root, "books", "results", "items")
        val books = array?.mapNotNull { element ->
            parseIssueBook(element)
        }.orEmpty()
        return BookSearchResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = books,
            meta = parseSearchMeta(root, books.size)
        )
    }

    private fun parseSearchMeta(root: JsonObject?, fallbackCount: Int): IssueSearchMeta? {
        val meta = root?.get("meta")?.asObjectOrNull()
            ?: root?.get("pagination")?.asObjectOrNull()
            ?: root?.get("data")?.asObjectOrNull()?.get("meta")?.asObjectOrNull()
            ?: root?.get("data")?.asObjectOrNull()?.get("pagination")?.asObjectOrNull()

        return if (meta != null) {
            IssueSearchMeta(
                currentPage = meta.int("current_page", "currentPage", "page"),
                lastPage = meta.int("last_page", "lastPage", "pages"),
                perPage = meta.int("per_page", "perPage", "page_size", "pageSize"),
                total = meta.int("total", "total_count", "totalCount")
            )
        } else {
            IssueSearchMeta(
                currentPage = 1,
                lastPage = 1,
                perPage = fallbackCount,
                total = fallbackCount
            )
        }
    }

    private fun parseIssueBook(element: JsonElement?): IssueBookItem? {
        val book = element.asObjectOrNull() ?: return null
        val id = book.int("id", "book_id", "bookId") ?: return null
        val title = book.string("title", "book_title", "name") ?: return null
        return IssueBookItem(
            id = id,
            bookId = book.int("book_id", "bookId"),
            issueId = book.int("copy_id", "book_copy_id", "accession_id"),
            title = title,
            author = book.string("author", "book_author", "author_name"),
            isbn = book.string("isbn"),
            accessionNo = book.string("accession_no", "accession_number", "accessionNo"),
            category = book.string("category", "category_name"),
            availableCopies = book.int("available_copies", "available_quantity", "available"),
            isAvailable = book.bool("is_available", "available_for_issue"),
            availabilityStatus = book.string("status", "availability_status", "availability"),
            coverImageUrl = book.string(
                "cover_image",
                "cover_image_url",
                "cover_url",
                "image_url",
                "thumbnail_url",
                "cover"
            ),
            reason = book.string("reason", "message")
        )
    }

    private fun parseIssueResponse(body: JsonElement?, fallbackCount: Int): IssueBooksResponse {
        val root = body.asObjectOrNull()
        val dataObject = root?.get("data")?.asObjectOrNull() ?: root
        val data = dataObject?.let {
            IssueBooksData(
                issuedCount = it.int("issued_count", "issuedCount", "count") ?: fallbackCount,
                dueDate = it.string("due_date") ?: it.string("dueDate"),
                issuedBooks = findListArray(it, "issued_books", "books", "issues")?.mapNotNull { element ->
                    runCatching { gson.fromJson(element, IssueBookItem::class.java) }.getOrNull()
                }
            )
        }
        return IssueBooksResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message") ?: "Books issued successfully.",
            data = data
        )
    }

    private fun parseIssueStudent(element: JsonElement?): IssueStudent? {
        val student = element.asObjectOrNull() ?: return null
        val user = student.get("user").asObjectOrNull()
        val profile = student.get("profile").asObjectOrNull()
        val media = student.get("media").asObjectOrNull()
        val id = student.int("id", "student_pk")
            ?: student.int("student_id_numeric", "user_id")
            ?: return null
        val name = student.string("name", "full_name", "student_name")
            ?: user?.string("name", "full_name")
            ?: profile?.string("name", "full_name")
            ?: return null
        return IssueStudent(
            id = id,
            name = name,
            studentId = student.string("student_id", "studentId", "admission_no", "registration_no")
                ?: profile?.string("student_id", "studentId", "admission_no", "registration_no"),
            rollNo = student.string("roll_no", "rollNo", "roll_number")
                ?: profile?.string("roll_no", "rollNo", "roll_number"),
            symbolNo = student.string("symbol_no", "symbolNo")
                ?: profile?.string("symbol_no", "symbolNo"),
            department = student.string("department", "faculty", "department_name", "program", "class", "section")
                ?: profile?.string("department", "faculty", "department_name", "program", "class", "section"),
            email = student.string("email") ?: user?.string("email") ?: profile?.string("email"),
            status = student.string("status", "account_status")
                ?: user?.string("status", "account_status"),
            profilePhotoUrl = student.string(
                "profile_photo_url",
                "profile_photo",
                "photo_url",
                "avatar",
                "avatar_url",
                "image_url",
                "photo",
                "photo_path",
                "profile_photo_path"
            )
                ?: user?.string(
                    "profile_photo_url",
                    "profile_photo",
                    "photo_url",
                    "avatar",
                    "avatar_url",
                    "image_url",
                    "photo",
                    "photo_path",
                    "profile_photo_path"
                )
                ?: profile?.string(
                    "profile_photo_url",
                    "profile_photo",
                    "photo_url",
                    "avatar",
                    "avatar_url",
                    "image_url",
                    "photo",
                    "photo_path",
                    "profile_photo_path"
                )
                ?: media?.string("url", "full_url", "preview_url"),
            canIssueBooks = student.bool("can_issue", "canIssue"),
            currentIssued = student.int("current_issued", "currently_issued", "issued_books", "already_issued")
                ?: profile?.int("current_issued", "currently_issued", "issued_books", "already_issued")
                ?: student.get("active_issued_books").asArrayOrNull()?.size(),
            maxBooks = student.int("max_books", "max_allowed_books", "issue_limit")
                ?: profile?.int("max_books", "max_allowed_books", "issue_limit")
        )
    }

    private fun fallbackPrivileges(student: IssueStudent, activeIssueCount: Int?): IssuePrivilegesData =
        IssuePrivilegesData(
            allowed = student.canIssueBooks ?: (!student.status.equals("inactive", ignoreCase = true) &&
                !student.status.equals("suspended", ignoreCase = true) &&
                !student.status.equals("blocked", ignoreCase = true)),
            reason = if (
                student.status.equals("inactive", ignoreCase = true) ||
                student.status.equals("suspended", ignoreCase = true) ||
                student.status.equals("blocked", ignoreCase = true)
            ) {
                "Student account status is ${student.status}."
            } else if (student.canIssueBooks == false) {
                "Student has reached the issue limit or is not eligible to issue books."
            } else {
                "Backend will validate issue limit, due date, fine rate, and book availability."
            },
            maxBooks = student.maxBooks,
            alreadyIssued = student.currentIssued ?: activeIssueCount,
            canIssue = student.maxBooks?.let { max ->
                (max - (student.currentIssued ?: activeIssueCount ?: 0)).coerceAtLeast(0)
            },
            backendValidated = true,
            settingsAvailable = false
        )

    private suspend fun <R> handleErrorResponse(response: Response<*>): NetworkResult<R> {
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            else -> NetworkResult.Error(parseErrorMessage(response), response.code())
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val errorBody = response.errorBody()?.string().orEmpty()
        if (errorBody.isBlank()) return response.message().ifBlank { Constants.ERROR_UNKNOWN }
        val apiMessage = runCatching {
            gson.fromJson(errorBody, ErrorResponse::class.java)?.message
        }.getOrNull()
        return apiMessage.takeUnless { it.isNullOrBlank() } ?: errorBody
    }

    private fun findListArray(root: JsonObject?, vararg preferredKeys: String): JsonArray? {
        if (root == null) return null
        preferredKeys.forEach { key ->
            root.get(key)?.asArrayOrNull()?.let { return it }
        }
        root.get("data")?.let { data ->
            data.asArrayOrNull()?.let { return it }
            val dataObject = data.asObjectOrNull()
            dataObject?.get("data")?.asArrayOrNull()?.let { return it }
            preferredKeys.forEach { key ->
                dataObject?.get(key)?.asArrayOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonElement?.asArrayOrNull(): JsonArray? =
        if (this != null && isJsonArray) asJsonArray else null

    private fun JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { value -> value.isNotBlank() }
        }

    private fun JsonObject.int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key ->
            runCatching { get(key)?.takeIf { !it.isJsonNull }?.asInt }.getOrNull()
        }

    private fun JsonObject.bool(vararg keys: String): Boolean? =
        keys.firstNotNullOfOrNull { key ->
            runCatching { get(key)?.takeIf { !it.isJsonNull }?.asBoolean }.getOrNull()
        }

    private fun Student.toIssueStudent(): IssueStudent =
        IssueStudent(
            id = id,
            name = name,
            studentId = studentId,
            department = department,
            email = email,
            status = status
        )

    private fun Book.toIssueBookItem(): IssueBookItem =
        IssueBookItem(
            id = id,
            title = title,
            author = author,
            isbn = isbn,
            accessionNo = accessionNo,
            category = category,
            availableCopies = availableCopies,
            isAvailable = (availableCopies ?: 0) > 0,
            availabilityStatus = availabilityStatus,
            coverImageUrl = cover_image
        )

    private companion object {
        const val TAG = "StaffIssueBookRepo"
        const val DEFAULT_PAGE_SIZE = 20
    }
}
