package com.saroj.lmsmobile.data.models.auth

import com.google.gson.annotations.SerializedName

/**
 * LoginRequest represents the login API request body.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

data class CompleteRegistrationRequest(
    val role: String,
    val email: String,
    val identifier: String,
    val phone: String,
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)

data class CompleteRegistrationResponse(
    val success: Boolean,
    val message: String,
    val data: CompleteRegistrationData? = null
)

data class CompleteRegistrationData(
    val role: String? = null,
    val email: String? = null
)

/**
 * LoginResponse represents the server response after successful login.
 * Contains the access token and user information.
 */
data class LoginResponse(
    val message: String? = null,
    val access_token: String,
    val token_type: String = "Bearer",
    val user: User
)

/**
 * User represents a user in the system.
 * Shared model used across login response and profile.
 */
data class User(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * ProfileResponse represents the profile API response.
 * Contains detailed user information.
 */
data class ProfileResponse(
    val message: String? = null,
    val user: User
)

