package com.saroj.lmsmobile.ui.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.auth.LoginResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * LoginViewModel handles login UI logic and state management.
 *
 * Responsibilities:
 * - Validate email and password input
 * - Trigger login API call
 * - Expose loading, error, and success states via LiveData
 * - Navigate based on user role
 *
 * Architecture:
 * - UI (LoginActivity) observes LiveData from ViewModel
 * - ViewModel calls Repository for data
 * - Repository makes API calls and handles errors
 * - Results flow back to ViewModel and update LiveData
 * - UI reacts to LiveData changes
 *
 * Usage:
 *   viewModel.loginResult.observe(this) { result ->
 *       when (result) {
 *           is NetworkResult.Success -> navigateToDashboard()
 *           is NetworkResult.Error -> showErrorDialog()
 *           is NetworkResult.Loading -> showLoadingDialog()
 *       }
 *   }
 *   viewModel.login(email, password)
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // ==================== UI State LiveData ====================

    /**
     * Holds the login result with all possible states.
     * UI observes this to show loading, error, or success states.
     */
    private val _loginResult = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginResult: LiveData<NetworkResult<LoginResponse>> = _loginResult

    /**
     * Holds email input errors.
     */
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    /**
     * Holds password input errors.
     */
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    /**
     * Holds a flag to show/hide password.
     */
    private val _showPassword = MutableLiveData(false)
    val showPassword: LiveData<Boolean> = _showPassword

    // ==================== Validation Methods ====================

    /**
     * Validates email format.
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    private fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validates password length.
     * @param password Password to validate
     * @return true if valid, false otherwise
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Validates all inputs before login.
     * @param email User email
     * @param password User password
     * @return true if all inputs are valid
     */
    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (!isEmailValid(email)) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        } else {
            _emailError.value = null
        }

        if (!isPasswordValid(password)) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        } else {
            _passwordError.value = null
        }

        return isValid
    }

    // ==================== Login Methods ====================

    /**
     * Initiates login process.
     *
     * Flow:
     * 1. Validate inputs
     * 2. Call repository.login()
     * 3. Collect results in viewModelScope
     * 4. Update LiveData with each result state
     *
     * @param email User email
     * @param password User password
     */
    fun login(email: String, password: String) {
        // Validate inputs
        if (!validateInputs(email, password)) {
            return
        }

        // Call repository and collect results
        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                _loginResult.value = result
            }
        }
    }

    /**
     * Toggles password visibility.
     */
    fun togglePasswordVisibility() {
        _showPassword.value = _showPassword.value?.not() ?: true
    }

    /**
     * Clears all validation errors.
     * Useful when user starts typing to clear previous error messages.
     */
    fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
    }
}

