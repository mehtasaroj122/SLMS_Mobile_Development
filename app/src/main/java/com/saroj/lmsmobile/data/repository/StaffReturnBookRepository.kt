package com.saroj.lmsmobile.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.issue.Issue
import com.saroj.lmsmobile.data.models.returnbook.ActiveIssueItem
import com.saroj.lmsmobile.data.models.returnbook.ConditionFines
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnData
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnRequest
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnResponse
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewData
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewItem
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewRequest
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewResponse
import com.saroj.lmsmobile.data.models.returnbook.ReturnRulesData
import com.saroj.lmsmobile.data.models.returnbook.ReturnSettingsResponse
import com.saroj.lmsmobile.data.models.returnbook.ReturnStudent
import com.saroj.lmsmobile.data.models.returnbook.ReturnStudentSearchResponse
import com.saroj.lmsmobile.data.models.returnbook.SingleReturnRequest
import com.saroj.lmsmobile.data.models.returnbook.StudentReturnData
import com.saroj.lmsmobile.data.models.returnbook.StudentReturnDataResponse
import com.saroj.lmsmobile.data.models.student.Student
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class StaffReturnBookRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getReturnSettings(): Flow<NetworkResult<ReturnSettingsResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getReturnSettings()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseSettings(response.body())))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(getLibrarySettingsFallback())
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Return settings failed", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun searchStudents(query: String): Flow<NetworkResult<ReturnStudentSearchResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.searchReturnStudents(query)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseStudentSearch(response.body())))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(searchStudentsFallback(query))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Return student search failed", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getStudentReturnData(student: ReturnStudent): Flow<NetworkResult<StudentReturnDataResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getStudentReturnData(student.id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseStudentReturnData(response.body(), student)))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(getStudentReturnDataFallback(student))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Student return data failed", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun previewFine(issueIds: List<Int>, condition: String): Flow<NetworkResult<ReturnPreviewResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.previewReturnFine(ReturnPreviewRequest(issueIds, condition))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parsePreview(response.body(), issueIds.size, condition)))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(NetworkResult.Error("Fine preview API is not available on the backend. Add POST /api/staff/returns/preview so Android can show backend-calculated fines before return.", response.code()))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Return preview failed", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun processReturn(issueIds: List<Int>, condition: String): Flow<NetworkResult<ProcessReturnResponse>> = flow {
        emit(NetworkResult.Loading())
        val request = ProcessReturnRequest(issueIds = issueIds, condition = condition)
        val response = apiService.processReturns(request)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseProcessReturn(response.body(), issueIds.size)))
        } else if (response.code() == Constants.HTTP_NOT_FOUND) {
            emit(returnOneByOne(issueIds, condition))
        } else {
            emit(handleErrorResponse(response))
        }
    }.catch { e ->
        Log.e(TAG, "Process return failed", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private suspend fun getLibrarySettingsFallback(): NetworkResult<ReturnSettingsResponse> {
        val response = apiService.getLibrarySettings()
        return if (response.isSuccessful) {
            NetworkResult.Success(parseSettings(response.body()))
        } else {
            handleErrorResponse(response)
        }
    }

    private suspend fun searchStudentsFallback(query: String): NetworkResult<ReturnStudentSearchResponse> {
        val response = apiService.searchStaffIssueStudents(query)
        return if (response.isSuccessful) {
            NetworkResult.Success(parseStudentSearch(response.body()))
        } else {
            handleErrorResponse(response)
        }
    }

    private suspend fun getStudentReturnDataFallback(student: ReturnStudent): NetworkResult<StudentReturnDataResponse> {
        val detailedStudent = loadStudentDetail(student)
        val issuesResponse = apiService.getStudentIssues(student.id, page = 1, pageSize = 100)
        if (!issuesResponse.isSuccessful) return handleErrorResponse(issuesResponse)
        val activeIssues = issuesResponse.body()?.data.orEmpty()
            .filter { issue -> issue.returnDate.isNullOrBlank() && !issue.status.equals("returned", ignoreCase = true) }
            .map { it.toActiveIssueItem() }
        val settings = runCatching {
            loadEffectiveRules(student.id)
        }.getOrNull()
        return NetworkResult.Success(
            StudentReturnDataResponse(
                success = true,
                message = "Return data fetched successfully.",
                data = StudentReturnData(
                    student = detailedStudent.copy(activeIssuesCount = activeIssues.size),
                    activeIssues = activeIssues,
                    returnRules = settings
                )
            )
        )
    }

    private suspend fun loadStudentDetail(baseStudent: ReturnStudent): ReturnStudent {
        var response = apiService.getStaffIssueStudentDetail(baseStudent.id)
        if (!response.isSuccessful && response.code() == Constants.HTTP_NOT_FOUND) {
            response = apiService.getStudentDetailJson(baseStudent.id)
        }
        if (!response.isSuccessful) return baseStudent
        val parsed = parseStudentDetail(response.body()) ?: return baseStudent
        return baseStudent.mergeWith(parsed)
    }

    private suspend fun loadEffectiveRules(studentId: Int): ReturnRulesData? {
        val privilegeResponse = apiService.getStaffStudentIssuePrivileges(studentId)
        if (privilegeResponse.isSuccessful) {
            val root = privilegeResponse.body().asObjectOrNull()
            val data = root?.get("data").asObjectOrNull() ?: root
            val privileges = data?.get("privileges").asObjectOrNull()
                ?: data?.get("rules").asObjectOrNull()
                ?: data
            parseRules(privileges)?.let { rules ->
                return rules.copy(hasCustomRules = rules.hasCustomRules || !rules.source.isNullOrBlank())
            }
        }
        val settingsResponse = apiService.getLibrarySettings()
        return if (settingsResponse.isSuccessful) parseSettings(settingsResponse.body()).data else null
    }

    private suspend fun returnOneByOne(issueIds: List<Int>, condition: String): NetworkResult<ProcessReturnResponse> {
        var returnedCount = 0
        var totalFine = 0.0
        for (issueId in issueIds) {
            val response = apiService.returnStaffIssue(
                issueId,
                SingleReturnRequest(condition = condition)
            )
            if (!response.isSuccessful) return handleErrorResponse(response)
            returnedCount++
            totalFine += parseProcessReturn(response.body(), 1).data?.totalFine ?: 0.0
        }
        return NetworkResult.Success(
            ProcessReturnResponse(
                success = true,
                message = "Book return processed successfully.",
                data = ProcessReturnData(returnedCount = returnedCount, totalFine = totalFine)
            )
        )
    }

    private fun parseSettings(body: JsonElement?): ReturnSettingsResponse {
        val root = body.asObjectOrNull()
        val data = root?.get("data").asObjectOrNull()
            ?: root?.get("settings").asObjectOrNull()
            ?: root?.get("return_rules").asObjectOrNull()
            ?: root
        return ReturnSettingsResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = parseRules(data)
        )
    }

    private fun parseStudentSearch(body: JsonElement?): ReturnStudentSearchResponse {
        val root = body.asObjectOrNull()
        val students = findListArray(root, "students", "results", "items", "borrowers")
            ?.mapNotNull(::parseStudent)
            .orEmpty()
        return ReturnStudentSearchResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = students
        )
    }

    private fun parseStudentDetail(body: JsonElement?): ReturnStudent? {
        val root = body.asObjectOrNull() ?: return null
        val data = root.get("data").asObjectOrNull()
        val student = data?.get("student").asObjectOrNull()
            ?: root.get("student").asObjectOrNull()
            ?: data
            ?: root
        return parseStudent(student)
    }

    private fun parseStudentReturnData(body: JsonElement?, fallbackStudent: ReturnStudent): StudentReturnDataResponse {
        val root = body.asObjectOrNull()
        val data = root?.get("data").asObjectOrNull() ?: root
        val student = data?.get("student")?.let(::parseStudent)?.let(fallbackStudent::mergeWith) ?: fallbackStudent
        val issues = findListArray(data, "active_issues", "activeIssues", "issued_books", "issues")
            ?.mapNotNull(::parseActiveIssue)
            .orEmpty()
        val rulesObject = data?.get("return_rules").asObjectOrNull()
            ?: data?.get("returnRules").asObjectOrNull()
            ?: data?.get("rules").asObjectOrNull()
            ?: data?.get("settings").asObjectOrNull()
        return StudentReturnDataResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = StudentReturnData(
                student = student.copy(activeIssuesCount = issues.size),
                activeIssues = issues,
                returnRules = parseRules(rulesObject)
            )
        )
    }

    private fun parsePreview(body: JsonElement?, fallbackCount: Int, fallbackCondition: String): ReturnPreviewResponse {
        val root = body.asObjectOrNull()
        val data = root?.get("data").asObjectOrNull() ?: root
        val items = findListArray(data, "items", "breakdown", "books", "calculations")
            ?.mapNotNull(::parsePreviewItem)
            .orEmpty()
        val rules = data?.get("rules").asObjectOrNull()
            ?: data?.get("return_rules").asObjectOrNull()
            ?: data?.get("returnRules").asObjectOrNull()
        return ReturnPreviewResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message"),
            data = ReturnPreviewData(
                totalFine = data?.double("total_fine", "totalFine", "fine_total", "amount") ?: 0.0,
                selectedCount = data?.int("selected_count", "selectedCount", "count") ?: fallbackCount,
                condition = data?.string("condition") ?: fallbackCondition,
                summary = data?.string("summary", "message", "calculation_summary"),
                items = items,
                rules = parseRules(rules)
            )
        )
    }

    private fun parseProcessReturn(body: JsonElement?, fallbackCount: Int): ProcessReturnResponse {
        val root = body.asObjectOrNull()
        val data = root?.get("data").asObjectOrNull() ?: root
        val issue = data?.get("issue").asObjectOrNull()
        val total = data?.double("total_fine", "totalFine", "fine_amount")
            ?: issue?.double("fine_amount", "total_fine")
            ?: 0.0
        val returned = data?.int("returned_count", "returnedCount", "count") ?: fallbackCount
        return ProcessReturnResponse(
            success = root?.bool("success"),
            status = root?.string("status"),
            message = root?.string("message") ?: "Book return processed successfully.",
            data = ProcessReturnData(returnedCount = returned, totalFine = total)
        )
    }

    private fun parseStudent(element: JsonElement?): ReturnStudent? {
        val student = element.asObjectOrNull() ?: return null
        val user = student.get("user").asObjectOrNull()
        val profile = student.get("profile").asObjectOrNull()
        val id = student.int("id", "student_pk")
            ?: student.int("student_id_numeric", "user_id")
            ?: return null
        val name = student.string("name", "full_name", "student_name")
            ?: user?.string("name", "full_name")
            ?: profile?.string("name", "full_name")
            ?: return null
        return ReturnStudent(
            id = id,
            userId = student.int("user_id", "userId") ?: user?.int("id"),
            name = name,
            studentId = student.string("student_id", "studentId", "admission_no", "registration_no")
                ?: profile?.string("student_id", "studentId", "admission_no", "registration_no"),
            rollNo = student.string("roll_no", "rollNo", "roll_number") ?: profile?.string("roll_no", "rollNo", "roll_number"),
            symbolNo = student.string("symbol_no", "symbolNo") ?: profile?.string("symbol_no", "symbolNo"),
            department = student.string("department", "faculty", "department_name", "program", "class", "section")
                ?: profile?.string("department", "faculty", "department_name", "program", "class", "section"),
            email = student.string("email") ?: user?.string("email") ?: profile?.string("email"),
            status = student.string("status", "account_status") ?: user?.string("status", "account_status"),
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
                ),
            activeIssuesCount = student.int("active_issues_count", "activeIssuesCount", "active_issued_books_count", "books_issued", "current_issued")
                ?: student.get("active_issued_books").asArrayOrNull()?.size()
        )
    }

    private fun parseActiveIssue(element: JsonElement?): ActiveIssueItem? {
        val source = element.asObjectOrNull() ?: return null
        val book = source.get("book").asObjectOrNull()
        val id = source.int("issue_id", "issueId", "id", "issued_book_id") ?: return null
        val title = source.string("book_title", "bookTitle", "title", "name")
            ?: book?.string("title", "book_title", "name")
            ?: return null
        return ActiveIssueItem(
            issueId = id,
            bookId = source.int("book_id", "bookId") ?: book?.int("id"),
            title = title,
            author = source.string("author", "book_author", "author_name") ?: book?.string("author", "book_author", "author_name"),
            isbn = source.string("isbn") ?: book?.string("isbn"),
            accessionNo = source.string("accession_no", "accessionNo", "accession_number") ?: book?.string("accession_no", "accessionNo", "accession_number"),
            issuedDate = source.string("issued_date", "issue_date", "issueDate", "issuedDate"),
            dueDate = source.string("due_date", "dueDate"),
            status = source.string("status"),
            overdueDays = source.int("overdue_days", "overdueDays", "days_overdue"),
            estimatedLateFine = source.double("estimated_late_fine", "estimatedFine", "estimated_fine")
        )
    }

    private fun parsePreviewItem(element: JsonElement?): ReturnPreviewItem? {
        val source = element.asObjectOrNull() ?: return null
        val id = source.int("issue_id", "issueId", "id", "issued_book_id") ?: return null
        val title = source.string("book_title", "bookTitle", "title") ?: "Selected book"
        return ReturnPreviewItem(
            issueId = id,
            bookTitle = title,
            issuedDate = source.string("issued_date", "issue_date", "issueDate", "issuedDate"),
            dueDate = source.string("due_date", "dueDate"),
            status = source.string("status"),
            overdueDays = source.int("overdue_days", "overdueDays", "days_overdue"),
            graceDays = source.int("grace_days", "graceDays"),
            chargeableOverdueDays = source.int("chargeable_overdue_days", "chargeableOverdueDays", "charged_days"),
            lateFine = source.double("late_fine", "lateFine"),
            conditionFine = source.double("condition_fine", "conditionFine"),
            bookTotal = source.double("book_total", "bookTotal", "total")
        )
    }

    private fun parseRules(source: JsonObject?): ReturnRulesData? {
        if (source == null) return null
        val condition = source.get("condition_fines").asObjectOrNull()
            ?: source.get("conditionFines").asObjectOrNull()
            ?: source
        return ReturnRulesData(
            issueDurationDays = source.int(
                "issue_duration_days",
                "duration_days",
                "issueDurationDays",
                "default_issue_duration",
                "issue_duration",
                "borrowing_days",
                "borrow_duration_days",
                "loan_period_days",
                "return_days",
                "max_issue_days"
            ),
            lateFinePerDay = source.double("late_fine_per_day", "fine_rate", "lateFinePerDay", "fine_per_day", "per_day_fine", "daily_fine"),
            graceDays = source.int("grace_days", "graceDays", "grace_period", "grace_period_days", "fine_grace_days"),
            fineCapPerBook = source.double("fine_cap_per_book", "fineCapPerBook", "fine_cap", "max_fine_per_book", "maximum_fine", "max_fine"),
            conditionFines = ConditionFines(
                good = condition.double("good", "good_fine", "good_condition_fine") ?: 0.0,
                fair = condition.double("fair", "fair_fine", "fair_condition_fine", "fair_book_fine", "fair_fine_amount", "condition_fair"),
                damaged = condition.double("damaged", "damaged_fine", "damage_fine", "damaged_book_fine", "damaged_fine_amount", "condition_damaged"),
                lost = condition.double("lost", "lost_fine", "lost_book_fine", "lost_fine_amount", "condition_lost")
            ),
            source = source.string("source", "rule_source", "policy_source", "policy_name"),
            hasCustomRules = source.bool("has_custom_rules", "hasCustomRules", "custom_privilege", "has_override") ?: false
        )
    }

    private suspend fun <R> handleErrorResponse(response: Response<*>): NetworkResult<R> =
        when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            else -> NetworkResult.Error(parseErrorMessage(response), response.code())
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
        preferredKeys.forEach { key -> root.get(key)?.asArrayOrNull()?.let { return it } }
        root.get("data")?.let { data ->
            data.asArrayOrNull()?.let { return it }
            val dataObject = data.asObjectOrNull()
            dataObject?.get("data")?.asArrayOrNull()?.let { return it }
            preferredKeys.forEach { key -> dataObject?.get(key)?.asArrayOrNull()?.let { return it } }
        }
        return null
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonElement?.asArrayOrNull(): JsonArray? =
        if (this != null && isJsonArray) asJsonArray else null

    private fun JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull }?.let { value ->
                runCatching { value.asString }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }

    private fun JsonObject.int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key -> runCatching { get(key)?.takeIf { !it.isJsonNull }?.asInt }.getOrNull() }

    private fun JsonObject.double(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key -> runCatching { get(key)?.takeIf { !it.isJsonNull }?.asDouble }.getOrNull() }

    private fun JsonObject.bool(vararg keys: String): Boolean? =
        keys.firstNotNullOfOrNull { key -> runCatching { get(key)?.takeIf { !it.isJsonNull }?.asBoolean }.getOrNull() }

    private fun Issue.toActiveIssueItem(): ActiveIssueItem =
        ActiveIssueItem(
            issueId = id,
            bookId = bookId,
            title = book?.title ?: "Book #$bookId",
            author = book?.author,
            isbn = book?.isbn,
            issuedDate = issueDate,
            dueDate = dueDate,
            status = status
        )

    private fun Student.toReturnStudent(): ReturnStudent =
        ReturnStudent(
            id = id,
            name = name,
            studentId = studentId,
            department = department,
            email = email,
            status = status
        )

    private companion object {
        const val TAG = "StaffReturnBookRepo"
    }
}
