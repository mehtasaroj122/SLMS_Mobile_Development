package com.saroj.lmsmobile.data.models.issue

import com.google.gson.annotations.SerializedName

data class StudentIssueContext(
    val student: IssueStudent,
    val privileges: IssuePrivilegesData
)

data class StudentSearchResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<IssueStudent> = emptyList(),
    val meta: IssueSearchMeta? = null
)

data class IssueSearchMeta(
    @SerializedName(value = "current_page", alternate = ["currentPage"])
    val currentPage: Int? = null,
    @SerializedName(value = "last_page", alternate = ["lastPage"])
    val lastPage: Int? = null,
    @SerializedName(value = "per_page", alternate = ["perPage"])
    val perPage: Int? = null,
    val total: Int? = null
)

data class IssueStudent(
    val id: Int,
    @SerializedName(value = "name", alternate = ["full_name", "student_name"])
    val name: String,
    @SerializedName(value = "student_id", alternate = ["studentId", "admission_no"])
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
    @SerializedName(value = "can_issue", alternate = ["canIssue"])
    val canIssueBooks: Boolean? = null,
    @SerializedName(value = "current_issued", alternate = ["currently_issued", "issued_books", "already_issued"])
    val currentIssued: Int? = null,
    @SerializedName(value = "max_books", alternate = ["max_allowed_books", "issue_limit"])
    val maxBooks: Int? = null
) {
    val displayIdentifier: String
        get() = listOfNotNull(studentId, rollNo, symbolNo).firstOrNull { it.isNotBlank() }.orEmpty()

    fun mergeWith(other: IssueStudent): IssueStudent =
        copy(
            name = other.name.ifBlank { name },
            studentId = other.studentId ?: studentId,
            rollNo = other.rollNo ?: rollNo,
            symbolNo = other.symbolNo ?: symbolNo,
            department = other.department ?: department,
            email = other.email ?: email,
            status = other.status ?: status,
            profilePhotoUrl = other.profilePhotoUrl ?: profilePhotoUrl,
            canIssueBooks = other.canIssueBooks ?: canIssueBooks,
            currentIssued = other.currentIssued ?: currentIssued,
            maxBooks = other.maxBooks ?: maxBooks
        )
}

data class IssuePrivilegesResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: IssuePrivilegesData? = null
)

data class IssuePrivilegesData(
    val allowed: Boolean = false,
    val reason: String? = null,
    @SerializedName(value = "max_books", alternate = ["maxBooks", "max_allowed_books", "issue_limit"])
    val maxBooks: Int? = null,
    @SerializedName(value = "already_issued", alternate = ["alreadyIssued", "current_issued", "currently_issued"])
    val alreadyIssued: Int? = null,
    @SerializedName(value = "can_issue", alternate = ["canIssue", "remaining", "remaining_books"])
    val canIssue: Int? = null,
    @SerializedName(value = "duration_days", alternate = ["durationDays", "duration", "issue_duration"])
    val durationDays: Int? = null,
    @SerializedName(value = "fine_rate", alternate = ["fineRate", "fine_per_day"])
    val fineRate: String? = null,
    @SerializedName(value = "issue_date", alternate = ["issueDate"])
    val issueDate: String? = null,
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    @SerializedName(value = "backend_validated", alternate = ["backendValidated"])
    val backendValidated: Boolean = false,
    @SerializedName(value = "settings_available", alternate = ["settingsAvailable"])
    val settingsAvailable: Boolean = true
) {
    val slotLimit: Int
        get() = (canIssue ?: maxBooks ?: 0).coerceAtLeast(0)
}

data class BookSearchResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<IssueBookItem> = emptyList(),
    val meta: IssueSearchMeta? = null
)

data class IssueBookItem(
    val id: Int,
    @SerializedName(value = "book_id", alternate = ["bookId"])
    val bookId: Int? = null,
    @SerializedName(value = "copy_id", alternate = ["book_copy_id", "accession_id"])
    val issueId: Int? = null,
    @SerializedName(value = "title", alternate = ["book_title", "name"])
    val title: String,
    @SerializedName(value = "author", alternate = ["book_author", "author_name"])
    val author: String? = null,
    val isbn: String? = null,
    @SerializedName(value = "accession_no", alternate = ["accession_number", "accessionNo"])
    val accessionNo: String? = null,
    val category: String? = null,
    @SerializedName(value = "available_copies", alternate = ["available_quantity", "available"])
    val availableCopies: Int? = null,
    @SerializedName(value = "is_available", alternate = ["available_for_issue"])
    val isAvailable: Boolean? = null,
    @SerializedName(value = "availability_status", alternate = ["availability", "status"])
    val availabilityStatus: String? = null,
    @SerializedName(
        value = "cover_image",
        alternate = ["cover_image_url", "cover_url", "image_url", "thumbnail_url", "cover"]
    )
    val coverImageUrl: String? = null,
    val reason: String? = null
) {
    val requestId: Int
        get() = bookId ?: issueId ?: id

    val isAlreadyIssued: Boolean
        get() = availabilityStatus.equals("already_issued", ignoreCase = true) ||
            availabilityStatus.equals("already issued", ignoreCase = true) ||
            availabilityStatus.equals("issued", ignoreCase = true) ||
            reason?.contains("already", ignoreCase = true) == true

    val canSelect: Boolean
        get() = when {
            isAlreadyIssued -> false
            availableCopies != null && availableCopies <= 0 -> false
            isAvailable != null -> isAvailable
            availabilityStatus != null -> availabilityStatus.equals("available", ignoreCase = true)
            else -> true
        }
}

data class IssueBooksRequest(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("book_ids")
    val bookIds: List<Int>
)

data class IssueBooksResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: IssueBooksData? = null
)

data class IssueBooksData(
    @SerializedName(value = "issued_count", alternate = ["issuedCount", "count"])
    val issuedCount: Int? = null,
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    @SerializedName(value = "issued_books", alternate = ["books", "issues"])
    val issuedBooks: List<IssueBookItem>? = null
)
