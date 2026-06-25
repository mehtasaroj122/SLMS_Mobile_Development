package com.saroj.lmsmobile.data.models.profile

data class ProfileUpdateRequest(
    val name: String,
    val email: String?,
    val phone: String?,
    val gender: String?,
    val address: String?
)

data class ChangePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String
)

data class DeleteAccountRequest(
    val current_password: String,
    val confirmation_text: String
)
