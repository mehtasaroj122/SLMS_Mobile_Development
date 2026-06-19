package com.saroj.lmsmobile.ui.student.model

data class MyRequestUiModel(
    val id: Int,
    val bookTitle: String,
    val author: String,
    val requestDate: String,
    val status: MyRequestStatus,
    val processedBy: String,
    val message: String,
    val coverImageUrl: String? = null,
    val requestDateSort: Long = 0L
)

enum class MyRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum class MyRequestsTab {
    ALL,
    PENDING,
    APPROVED,
    REJECTED
}

data class MyRequestsSummaryUiModel(
    val totalRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val rejectedRequests: Int
)

data class MyRequestActionResult(
    val message: String,
    val request: MyRequestUiModel? = null
)
