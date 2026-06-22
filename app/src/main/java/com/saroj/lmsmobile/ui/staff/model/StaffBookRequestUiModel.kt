package com.saroj.lmsmobile.ui.staff.model

data class StaffBookRequestsSummaryUiModel(
    val totalRequests: Int = 0,
    val pendingRequests: Int = 0,
    val approvedRequests: Int = 0,
    val rejectedRequests: Int = 0
)

data class StaffBookRequestUiModel(
    val id: Int,
    val studentName: String,
    val studentIdentifier: String,
    val studentDepartment: String,
    val studentPhotoUrl: String?,
    val bookTitle: String,
    val author: String,
    val requestDate: String,
    val processedBy: String,
    val processedAt: String,
    val status: StaffBookRequestStatus,
    val sortDateMillis: Long
)

data class StaffBookRequestActionResult(
    val message: String,
    val request: StaffBookRequestUiModel?
)

enum class StaffBookRequestStatus(val apiValue: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    fun displayLabel(): String = when (this) {
        PENDING -> "Pending"
        APPROVED -> "Approved"
        REJECTED -> "Rejected"
    }
}

enum class StaffBookRequestsTab(val apiValue: String) {
    ALL("all"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected")
}
