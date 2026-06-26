package com.saroj.lmsmobile.ui.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.auth.ForgotPasswordResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forgotPasswordResult = MutableLiveData<NetworkResult<ForgotPasswordResponse>>()
    val forgotPasswordResult: LiveData<NetworkResult<ForgotPasswordResponse>> = _forgotPasswordResult

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private var isSending = false

    fun sendResetLink(email: String) {
        if (isSending) return

        val trimmedEmail = email.trim()
        if (!validateEmail(trimmedEmail)) return

        viewModelScope.launch {
            authRepository.forgotPassword(trimmedEmail).collect { result ->
                isSending = result is NetworkResult.Loading
                _forgotPasswordResult.value = result
            }
        }
    }

    fun clearErrors() {
        _emailError.value = null
    }

    private fun validateEmail(email: String): Boolean {
        val error = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address"
            else -> null
        }

        _emailError.value = error
        return error == null
    }
}
