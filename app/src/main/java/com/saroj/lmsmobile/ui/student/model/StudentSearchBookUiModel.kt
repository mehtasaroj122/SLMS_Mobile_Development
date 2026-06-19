package com.saroj.lmsmobile.ui.student.model

data class StudentSearchBookUiModel(
    val id: Int,
    val title: String,
    val author: String,
    val category: String,
    val publisher: String,
    val condition: String,
    val accessionNo: String,
    val location: String,
    val quantity: Int,
    val availableQuantity: Int,
    val availabilityStatus: String,
    val coverImageUrl: String?,
    val requestState: BookRequestState
)

enum class BookRequestState {
    NONE,
    UNAVAILABLE,
    PENDING,
    APPROVED,
    ALREADY_ISSUED,
    REJECTED,
    LOADING
}
