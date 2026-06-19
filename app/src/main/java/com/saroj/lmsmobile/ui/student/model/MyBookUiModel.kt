package com.saroj.lmsmobile.ui.student.model

data class MyBookUiModel(
    val issueId: Int,
    val title: String,
    val author: String,
    val isbn: String,
    val issueDate: String,
    val dueDate: String,
    val returnDate: String?,
    val status: MyBookStatus,
    val fineAmount: String,
    val fineStatus: FineStatus,
    val coverImageUrl: String? = null,
    val issueDateSort: Long = 0L,
    val dueDateSort: Long = 0L,
    val fineAmountValue: Double = 0.0
)

enum class MyBookStatus {
    ISSUED,
    DUE_SOON,
    OVERDUE,
    RETURNED
}

enum class FineStatus {
    NONE,
    UNPAID,
    PAID,
    WAIVED
}

enum class MyBooksTab {
    CURRENT,
    HISTORY,
    DUE_SOON
}

data class MyBooksSummaryUiModel(
    val totalIssued: Int,
    val currentlyBorrowed: Int,
    val overdueBooks: Int,
    val pendingFine: String,
    val pendingFineValue: Double = 0.0
)

enum class MyBooksStatusFilter(val label: String, val status: MyBookStatus?) {
    ALL("All Status", null),
    ISSUED("Issued", MyBookStatus.ISSUED),
    OVERDUE("Overdue", MyBookStatus.OVERDUE),
    DUE_SOON("Due Soon", MyBookStatus.DUE_SOON),
    RETURNED("Returned", MyBookStatus.RETURNED)
}

enum class MyBooksFineFilter(val label: String, val status: FineStatus?) {
    ALL("All Fine Status", null),
    PAID("Paid", FineStatus.PAID),
    UNPAID("Unpaid", FineStatus.UNPAID),
    WAIVED("Waived", FineStatus.WAIVED)
}

enum class MyBooksSortOption(val label: String) {
    DUE_DATE_ASC("Due Date (Asc)"),
    DUE_DATE_DESC("Due Date (Desc)"),
    ISSUE_DATE("Issue Date"),
    FINE_AMOUNT("Fine Amount")
}
