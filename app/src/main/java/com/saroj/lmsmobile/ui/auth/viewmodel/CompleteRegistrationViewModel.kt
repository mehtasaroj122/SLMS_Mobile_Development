package com.saroj.lmsmobile.ui.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.auth.CompleteRegistrationRequest
import com.saroj.lmsmobile.data.repository.CompleteRegistrationRepository
import com.saroj.lmsmobile.data.repository.CompleteRegistrationResult
import kotlinx.coroutines.launch

class CompleteRegistrationViewModel(
    private val repository: CompleteRegistrationRepository
) : ViewModel() {

    private val _state = MutableLiveData<CompleteRegistrationUiState>(CompleteRegistrationUiState.Idle)
    val state: LiveData<CompleteRegistrationUiState> = _state

    private val _fieldErrors = MutableLiveData<Map<String, String>>(emptyMap())
    val fieldErrors: LiveData<Map<String, String>> = _fieldErrors

    fun completeRegistration(
        role: String,
        email: String,
        identifier: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        val normalizedRole = role.lowercase().trim()
        val errors = validateInputs(
            role = normalizedRole,
            email = email.trim(),
            identifier = identifier.trim(),
            phone = phone.trim(),
            password = password,
            confirmPassword = confirmPassword
        )

        if (errors.isNotEmpty()) {
            _fieldErrors.value = errors
            _state.value = CompleteRegistrationUiState.Idle
            return
        }

        viewModelScope.launch {
            repository.completeRegistration(
                CompleteRegistrationRequest(
                    role = normalizedRole,
                    email = email.trim(),
                    identifier = identifier.trim(),
                    phone = phone.trim(),
                    password = password,
                    passwordConfirmation = confirmPassword
                )
            ).collect { result ->
                when (result) {
                    CompleteRegistrationResult.Loading -> _state.value = CompleteRegistrationUiState.Loading
                    is CompleteRegistrationResult.Success -> {
                        _fieldErrors.value = emptyMap()
                        _state.value = CompleteRegistrationUiState.Success(result.response.message)
                    }
                    is CompleteRegistrationResult.ValidationError -> {
                        _fieldErrors.value = flattenErrors(result.errors)
                        _state.value = CompleteRegistrationUiState.Error(
                            result.errors.values.firstOrNull()?.firstOrNull() ?: result.message
                        )
                    }
                    is CompleteRegistrationResult.Error -> {
                        _state.value = CompleteRegistrationUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun clearFieldError(field: String) {
        val currentErrors = _fieldErrors.value.orEmpty()
        if (field !in currentErrors) return
        _fieldErrors.value = currentErrors - field
    }

    fun clearIdentifierError() {
        val currentErrors = _fieldErrors.value.orEmpty()
        _fieldErrors.value = currentErrors - "identifier" - "student_id" - "staff_id"
    }

    fun clearAllErrors() {
        _fieldErrors.value = emptyMap()
    }

    private fun validateInputs(
        role: String,
        email: String,
        identifier: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (role !in setOf("student", "staff")) {
            errors["role"] = "Select the role linked to your invitation."
        }

        if (email.isBlank()) {
            errors["email"] = "Enter the email address linked to your account."
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Enter a valid email address."
        }

        if (identifier.isBlank()) {
            errors["identifier"] = if (role == "staff") "Enter your staff ID." else "Enter your student ID."
        }

        if (phone.isBlank()) {
            errors["phone"] = "Enter the phone number linked to your invitation."
        }

        if (password.isBlank()) {
            errors["password"] = "Create a password for your account."
        } else if (password.length < 8) {
            errors["password"] = "Password must be at least 8 characters long."
        } else if (
            !password.any { it.isUpperCase() } ||
            !password.any { it.isLowerCase() } ||
            !password.any { it.isDigit() } ||
            !password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        ) {
            errors["password"] = "Password must include uppercase, lowercase, number, and symbol."
        }

        if (confirmPassword.isBlank()) {
            errors["password_confirmation"] = "Confirm your password."
        } else if (password != confirmPassword) {
            errors["password"] = "Password confirmation does not match."
        }

        return errors
    }

    private fun flattenErrors(errors: Map<String, List<String>>): Map<String, String> {
        return errors.mapValues { (_, messages) -> messages.firstOrNull().orEmpty() }
            .filterValues { it.isNotBlank() }
    }
}

sealed class CompleteRegistrationUiState {
    object Idle : CompleteRegistrationUiState()
    object Loading : CompleteRegistrationUiState()
    data class Success(val message: String) : CompleteRegistrationUiState()
    data class Error(val message: String) : CompleteRegistrationUiState()
}
