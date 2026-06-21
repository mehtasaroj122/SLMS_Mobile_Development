package com.saroj.lmsmobile.data.models.fine

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Fine represents a fine imposed on a student for overdue books.
 */
data class Fine(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("issue_id")
    val issueId: Int? = null,
    val amount: Double,
    @SerializedName("fine_date")
    val fineDate: String,
    @SerializedName("paid_date")
    val paidDate: String? = null,
    val status: String, // pending, paid, waived
    val remarks: String? = null,
    val student: FineStudent? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable {
    val isPaid: Boolean
        get() = status == "paid"

    val isPending: Boolean
        get() = status == "pending"
}

/**
 * FineResponse wraps fine data from API.
 */
data class FineResponse(
    val message: String? = null,
    val data: Fine
)

/**
 * Nested Student in Fine response.
 */
data class FineStudent(
    val id: Int,
    val name: String,
    @SerializedName("student_id")
    val studentId: String? = null,
    val email: String? = null
) : Serializable

data class FineSummaryResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: FineSummaryData? = null
)

data class FineSummaryData(
    @SerializedName("total_fines")
    val totalFines: Double = 0.0,
    val collected: Double = 0.0,
    val pending: Double = 0.0,
    val waived: Double = 0.0,
    @SerializedName("total_records")
    val totalRecords: Int = 0,
    @SerializedName("paid_records")
    val paidRecords: Int = 0,
    @SerializedName("pending_records")
    val pendingRecords: Int = 0,
    @SerializedName("waived_records")
    val waivedRecords: Int = 0,
    @SerializedName("overdue_records")
    val overdueRecords: Int = 0
)

data class FineStudentListResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: List<FineStudentSummary> = emptyList()
)

data class FineStudentSummary(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("user_id")
    val userId: Int? = null,
    val name: String? = null,
    @SerializedName("roll_no")
    val rollNo: String? = null,
    @SerializedName("symbol_no")
    val symbolNo: String? = null,
    @SerializedName("student_code")
    val studentCode: String? = null,
    val department: String? = null,
    val email: String? = null,
    val photo: String? = null,
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    @SerializedName("total_fine")
    val totalFine: Double = 0.0,
    @SerializedName("pending_amount")
    val pendingAmount: Double = 0.0,
    @SerializedName("paid_amount")
    val paidAmount: Double = 0.0,
    @SerializedName("waived_amount")
    val waivedAmount: Double = 0.0,
    @SerializedName("fine_records_count")
    val fineRecordsCount: Int = 0,
    @SerializedName("pending_records_count")
    val pendingRecordsCount: Int = 0,
    @SerializedName("paid_records_count")
    val paidRecordsCount: Int = 0,
    @SerializedName("waived_records_count")
    val waivedRecordsCount: Int = 0,
    @SerializedName("overdue_records_count")
    val overdueRecordsCount: Int = 0,
    val status: String? = null
) : Serializable {
    val displayIdentifier: String
        get() = listOf(rollNo, symbolNo, studentCode)
            .firstOrNull { !it.isNullOrBlank() }
            ?: "Student #$studentId"

    val displayPhoto: String?
        get() = profilePhotoUrl ?: photo
}

data class StudentFineDetailResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: StudentFineDetailData? = null
)

data class StudentFineDetailData(
    val student: FineStudentInfo? = null,
    val summary: StudentFineSummary? = null,
    val fines: List<FineRecord> = emptyList()
)

data class FineStudentInfo(
    val id: Int? = null,
    @SerializedName("student_id")
    val studentId: Int? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    val name: String? = null,
    @SerializedName("roll_no")
    val rollNo: String? = null,
    @SerializedName("symbol_no")
    val symbolNo: String? = null,
    @SerializedName("student_code")
    val studentCode: String? = null,
    val department: String? = null,
    val email: String? = null,
    val photo: String? = null,
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null
) : Serializable {
    val displayId: Int
        get() = id ?: studentId ?: 0

    val displayIdentifier: String
        get() = listOf(rollNo, symbolNo, studentCode)
            .firstOrNull { !it.isNullOrBlank() }
            ?: if (displayId > 0) "Student #$displayId" else "-"

    val displayPhoto: String?
        get() = profilePhotoUrl ?: photo
}

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
    val recordsCount: Int = 0,
    @SerializedName("pending_records")
    val pendingRecords: Int = 0,
    @SerializedName("paid_records")
    val paidRecords: Int = 0,
    @SerializedName("waived_records")
    val waivedRecords: Int = 0,
    @SerializedName("overdue_records")
    val overdueRecords: Int = 0
)

data class FineRecord(
    val id: Int,
    @SerializedName("fine_id")
    val fineId: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    val author: String? = null,
    val isbn: String? = null,
    @SerializedName("cover_image")
    val coverImage: String? = null,
    @SerializedName("cover_image_url")
    val coverImageUrl: String? = null,
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
    val waivedAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
) : Serializable {
    val resolvedId: Int
        get() = fineId ?: id

    val resolvedDaysOverdue: Int
        get() = daysLate ?: daysOverdue

    val resolvedReason: String?
        get() = reason ?: fineType ?: remarks

    val displayCover: String?
        get() = coverImageUrl ?: coverImage
}

data class WaiveFineRequest(
    val reason: String
)

data class FineActionResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val data: FineRecord? = null
)

