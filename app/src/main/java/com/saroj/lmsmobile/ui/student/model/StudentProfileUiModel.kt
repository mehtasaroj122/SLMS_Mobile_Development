package com.saroj.lmsmobile.ui.student.model

data class StudentProfileUiModel(
    val name: String,
    val email: String,
    val role: String,
    val username: String,
    val studentId: String,
    val department: String,
    val memberSince: String,
    val lastLogin: String,
    val phone: String,
    val gender: String,
    val address: String,
    val profilePhotoUrl: String?
)

data class DeleteEligibilityUiModel(
    val canDelete: Boolean,
    val issuedBooks: Int,
    val pendingFines: String,
    val activeRequests: Int,
    val reasons: List<String>
)

enum class ProfileTab {
    PROFILE,
    PHOTO,
    SECURITY,
    DELETE
}
