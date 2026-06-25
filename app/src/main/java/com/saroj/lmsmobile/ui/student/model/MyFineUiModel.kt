package com.saroj.lmsmobile.ui.student.model

data class MyFineUiModel(
    val id: Int,
    val bookTitle: String,
    val reason: String,
    val dueDate: String,
    val daysOverdue: String,
    val amount: String,
    val status: MyFineStatus,
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val coverImageUrl: String? = null,
    val amountValue: Double = 0.0,
    val dueDateSort: Long = 0L,
    val issueId: Int? = null
)

enum class MyFineStatus {
    PENDING,
    PAID,
    WAIVED
}

enum class MyFinesTab {
    ALL,
    PENDING,
    PAID,
    WAIVED
}

data class MyFinesSummaryUiModel(
    val outstandingAmount: String,
    val outstandingCount: Int,
    val paidAmount: String,
    val paidCount: Int,
    val waivedAmount: String,
    val waivedCount: Int,
    val overdueBooks: Int,
    val outstandingAmountValue: Double = 0.0,
    val paidAmountValue: Double = 0.0,
    val waivedAmountValue: Double = 0.0
)
