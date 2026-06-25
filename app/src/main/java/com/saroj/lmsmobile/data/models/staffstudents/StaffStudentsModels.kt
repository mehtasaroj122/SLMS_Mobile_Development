package com.saroj.lmsmobile.data.models.staffstudents

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StaffStudentsResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<StaffStudentItem> = emptyList()
)

data class StaffStudentItem(
    val id: Int = 0,
    @SerializedName("user_id")
    val userId: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("roll_no")
    val rollNo: String? = null,
    @SerializedName("symbol_no")
    val symbolNo: String? = null,
    @SerializedName("student_code")
    val studentCode: String? = null,
    val department: String? = null,
    val faculty: String? = null,
    val batch: String? = null,
    val status: String? = null,
    val photo: String? = null,
    @SerializedName("profile_photo")
    val profilePhoto: String? = null,
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null
) : Serializable {
    val displayIdentifier: String
        get() = listOf(studentId, rollNo, symbolNo, studentCode)
            .firstOrNull { !it.isNullOrBlank() }
            ?: if (id > 0) "Student #$id" else "-"

    val displayDepartment: String
        get() = department?.takeIf { it.isNotBlank() }
            ?: faculty?.takeIf { it.isNotBlank() }
            ?: "-"

    val displayPhoto: String?
        get() = profilePhotoUrl ?: photo ?: profilePhoto

    val normalizedStatus: String
        get() = status?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: "active"
}

data class StaffStudentDetailResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: StaffStudentDetailData? = null
)

data class StaffStudentDetailData(
    val student: StaffStudentItem? = null
)

data class StudentIssuedBooksResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<StudentIssuedBookItem> = emptyList()
)

data class StudentIssuedBookItem(
    @SerializedName("issue_id")
    val issueId: Int = 0,
    @SerializedName("book_id")
    val bookId: Int? = null,
    val title: String? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    val author: String? = null,
    val isbn: String? = null,
    @SerializedName("accession_no")
    val accessionNo: String? = null,
    @SerializedName("issue_date")
    val issueDate: String? = null,
    @SerializedName("issued_date")
    val issuedDate: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("return_date")
    val returnDate: String? = null,
    val status: String? = null,
    @SerializedName("fine_amount")
    val fineAmount: Double = 0.0
) : Serializable {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: bookTitle?.takeIf { it.isNotBlank() } ?: "Untitled Book"

    val displayIssueDate: String?
        get() = issuedDate ?: issueDate
}

data class StudentFinesResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: StudentFinesData? = null
)

data class StudentFinesData(
    val student: StaffStudentItem? = null,
    val summary: StudentFineSummary = StudentFineSummary(),
    val fines: List<StudentFineItem> = emptyList()
)

data class StudentFineSummary(
    @SerializedName("total_fine")
    val totalFine: Double = 0.0,
    @SerializedName("pending_amount")
    val pendingAmount: Double = 0.0,
    @SerializedName("paid_amount")
    val paidAmount: Double = 0.0,
    @SerializedName("waived_amount")
    val waivedAmount: Double = 0.0,
    @SerializedName("records_count")
    val recordsCount: Int = 0
)

data class StudentFineItem(
    val id: Int = 0,
    @SerializedName("fine_id")
    val fineId: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    val author: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("return_date")
    val returnDate: String? = null,
    @SerializedName("days_overdue")
    val daysOverdue: Int = 0,
    @SerializedName("days_late")
    val daysLate: Int? = null,
    val amount: Double = 0.0,
    val status: String? = null,
    val reason: String? = null,
    @SerializedName("fine_type")
    val fineType: String? = null,
    val remarks: String? = null,
    @SerializedName("waive_reason")
    val waiveReason: String? = null,
    @SerializedName("paid_at")
    val paidAt: String? = null,
    @SerializedName("paid_date")
    val paidDate: String? = null,
    @SerializedName("waived_at")
    val waivedAt: String? = null
) : Serializable {
    val resolvedId: Int
        get() = fineId ?: id

    val resolvedDaysLate: Int
        get() = daysLate ?: daysOverdue

    val resolvedReason: String?
        get() = reason ?: fineType ?: remarks
}

data class StudentBookRequestsResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<StudentBookRequestItem> = emptyList()
)

data class StudentBookRequestItem(
    val id: Int = 0,
    @SerializedName("request_id")
    val requestId: Int? = null,
    val book: RequestBookInfo? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    val author: String? = null,
    @SerializedName("request_date")
    val requestDate: String? = null,
    @SerializedName("requested_at")
    val requestedAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val status: String? = null,
    val message: String? = null,
    @SerializedName("processed_by")
    val processedBy: String? = null,
    @SerializedName("processed_by_name")
    val processedByName: String? = null,
    @SerializedName("processed_at")
    val processedAt: String? = null,
    @SerializedName("processed_date")
    val processedDate: String? = null
) : Serializable {
    val resolvedId: Int
        get() = requestId ?: id

    val displayBookTitle: String
        get() = book?.title?.takeIf { it.isNotBlank() }
            ?: bookTitle?.takeIf { it.isNotBlank() }
            ?: "Untitled Book"

    val displayAuthor: String?
        get() = book?.author ?: author

    val displayRequestDate: String?
        get() = requestDate ?: requestedAt ?: createdAt

    val displayProcessedBy: String?
        get() = processedByName ?: processedBy

    val displayProcessedAt: String?
        get() = processedAt ?: processedDate
}

data class RequestBookInfo(
    val id: Int? = null,
    val title: String? = null,
    val author: String? = null
) : Serializable

data class StaffStudentActionResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null
)
