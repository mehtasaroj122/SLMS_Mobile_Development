package com.saroj.lmsmobile.data.models.returnbook

import com.google.gson.annotations.SerializedName

data class ReturnSettingsResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: ReturnRulesData? = null
)

data class ReturnStudentSearchResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<ReturnStudent> = emptyList()
)

data class StudentReturnDataResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: StudentReturnData? = null
)

data class StudentReturnData(
    val student: ReturnStudent,
    @SerializedName(value = "active_issues", alternate = ["activeIssues", "issues", "issued_books"])
    val activeIssues: List<ActiveIssueItem> = emptyList(),
    @SerializedName(value = "return_rules", alternate = ["returnRules", "rules", "settings"])
    val returnRules: ReturnRulesData? = null
)

data class ReturnStudent(
    val id: Int,
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: Int? = null,
    @SerializedName(value = "name", alternate = ["full_name", "student_name"])
    val name: String,
    @SerializedName(value = "student_id", alternate = ["studentId", "admission_no", "registration_no"])
    val studentId: String? = null,
    @SerializedName(value = "roll_no", alternate = ["rollNo", "roll_number"])
    val rollNo: String? = null,
    @SerializedName(value = "symbol_no", alternate = ["symbolNo"])
    val symbolNo: String? = null,
    @SerializedName(value = "department", alternate = ["faculty", "department_name", "program", "class", "section"])
    val department: String? = null,
    val email: String? = null,
    val status: String? = null,
    @SerializedName(
        value = "profile_photo_url",
        alternate = ["profile_photo", "photo_url", "avatar", "avatar_url", "image_url", "photo", "photo_path", "profile_photo_path"]
    )
    val profilePhotoUrl: String? = null,
    @SerializedName(value = "active_issues_count", alternate = ["activeIssuesCount", "active_issued_books_count", "books_issued", "current_issued"])
    val activeIssuesCount: Int? = null
) {
    val displayIdentifier: String
        get() = listOfNotNull(studentId, rollNo, symbolNo).firstOrNull { it.isNotBlank() }.orEmpty()

    fun mergeWith(other: ReturnStudent): ReturnStudent =
        copy(
            userId = other.userId ?: userId,
            name = other.name.ifBlank { name },
            studentId = other.studentId ?: studentId,
            rollNo = other.rollNo ?: rollNo,
            symbolNo = other.symbolNo ?: symbolNo,
            department = other.department ?: department,
            email = other.email ?: email,
            status = other.status ?: status,
            profilePhotoUrl = other.profilePhotoUrl ?: profilePhotoUrl,
            activeIssuesCount = other.activeIssuesCount ?: activeIssuesCount
        )
}

data class ActiveIssueItem(
    @SerializedName(value = "issue_id", alternate = ["issueId", "id", "issued_book_id"])
    val issueId: Int,
    @SerializedName(value = "book_id", alternate = ["bookId"])
    val bookId: Int? = null,
    @SerializedName(value = "title", alternate = ["book_title", "bookTitle", "name"])
    val title: String,
    @SerializedName(value = "author", alternate = ["book_author", "author_name"])
    val author: String? = null,
    val isbn: String? = null,
    @SerializedName(value = "accession_no", alternate = ["accessionNo", "accession_number"])
    val accessionNo: String? = null,
    @SerializedName(value = "issued_date", alternate = ["issue_date", "issueDate", "issuedDate"])
    val issuedDate: String? = null,
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    val status: String? = null,
    @SerializedName(value = "overdue_days", alternate = ["overdueDays", "days_overdue"])
    val overdueDays: Int? = null,
    @SerializedName(value = "estimated_late_fine", alternate = ["estimatedFine", "estimated_fine"])
    val estimatedLateFine: Double? = null
)

data class ReturnRulesData(
    @SerializedName(value = "issue_duration_days", alternate = ["duration_days", "issueDurationDays", "default_issue_duration", "issue_duration"])
    val issueDurationDays: Int? = null,
    @SerializedName(value = "late_fine_per_day", alternate = ["fine_rate", "lateFinePerDay", "fine_per_day", "per_day_fine"])
    val lateFinePerDay: Double? = null,
    @SerializedName(value = "grace_days", alternate = ["graceDays", "grace_period"])
    val graceDays: Int? = null,
    @SerializedName(value = "fine_cap_per_book", alternate = ["fineCapPerBook", "fine_cap", "max_fine_per_book"])
    val fineCapPerBook: Double? = null,
    @SerializedName(value = "condition_fines", alternate = ["conditionFines"])
    val conditionFines: ConditionFines = ConditionFines(),
    @SerializedName(value = "source", alternate = ["rule_source", "policy_source"])
    val source: String? = null,
    @SerializedName(value = "has_custom_rules", alternate = ["hasCustomRules", "custom_privilege", "has_override"])
    val hasCustomRules: Boolean = false
)

data class ConditionFines(
    val good: Double? = null,
    val fair: Double? = null,
    val damaged: Double? = null,
    val lost: Double? = null
) {
    fun amountFor(condition: String): Double? = when (condition.lowercase()) {
        "good" -> good
        "fair" -> fair
        "damaged" -> damaged
        "lost" -> lost
        else -> null
    }
}

data class ReturnPreviewRequest(
    @SerializedName("issue_ids")
    val issueIds: List<Int>,
    val condition: String
)

data class ReturnPreviewResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: ReturnPreviewData? = null
)

data class ReturnPreviewData(
    @SerializedName(value = "total_fine", alternate = ["totalFine"])
    val totalFine: Double = 0.0,
    @SerializedName(value = "selected_count", alternate = ["selectedCount", "count"])
    val selectedCount: Int = 0,
    val condition: String? = null,
    val summary: String? = null,
    val items: List<ReturnPreviewItem> = emptyList(),
    val rules: ReturnRulesData? = null
)

data class ReturnPreviewItem(
    @SerializedName(value = "issue_id", alternate = ["issueId", "id", "issued_book_id"])
    val issueId: Int,
    @SerializedName(value = "book_title", alternate = ["bookTitle", "title"])
    val bookTitle: String,
    @SerializedName(value = "issued_date", alternate = ["issue_date", "issueDate", "issuedDate"])
    val issuedDate: String? = null,
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    val status: String? = null,
    @SerializedName(value = "overdue_days", alternate = ["overdueDays", "days_overdue"])
    val overdueDays: Int? = null,
    @SerializedName(value = "grace_days", alternate = ["graceDays"])
    val graceDays: Int? = null,
    @SerializedName(value = "chargeable_overdue_days", alternate = ["chargeableOverdueDays", "charged_days"])
    val chargeableOverdueDays: Int? = null,
    @SerializedName(value = "late_fine", alternate = ["lateFine"])
    val lateFine: Double? = null,
    @SerializedName(value = "condition_fine", alternate = ["conditionFine"])
    val conditionFine: Double? = null,
    @SerializedName(value = "book_total", alternate = ["bookTotal", "total"])
    val bookTotal: Double? = null
)

data class ProcessReturnRequest(
    @SerializedName("issue_ids")
    val issueIds: List<Int>,
    val condition: String,
    val notes: String? = null
)

data class SingleReturnRequest(
    val condition: String,
    val notes: String? = null
)

data class ProcessReturnResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: ProcessReturnData? = null
)

data class ProcessReturnData(
    @SerializedName(value = "returned_count", alternate = ["returnedCount", "count"])
    val returnedCount: Int = 0,
    @SerializedName(value = "total_fine", alternate = ["totalFine"])
    val totalFine: Double = 0.0,
    @SerializedName(value = "returned_books", alternate = ["returnedBooks", "books", "issues"])
    val returnedBooks: List<ActiveIssueItem> = emptyList(),
    val fines: List<Any>? = null
)
