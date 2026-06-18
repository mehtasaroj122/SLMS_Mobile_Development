package com.saroj.lmsmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.AuthRepository
import com.saroj.lmsmobile.ui.admin.AdminDashboardActivity
import com.saroj.lmsmobile.ui.auth.viewmodel.LoginViewModel
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.student.StudentDashboardActivity
import com.saroj.lmsmobile.utils.Constants

/**
 * LoginActivity handles user authentication.
 *
 * Features:
 * - Email and password input fields
 * - Password visibility toggle
 * - Input validation with error messages
 * - Loading state indicator
 * - Role-based navigation after successful login
 * - Error handling with user-friendly messages
 *
 * UI Components:
 * - EditText for email
 * - EditText for password
 * - Button to toggle password visibility
 * - Button for login
 * - ProgressBar for loading state
 * - TextViews for error messages
 *
 * Architecture:
 * LoginActivity → LoginViewModel → AuthRepository → ApiService → API
 *
 * User Flow:
 * 1. Enter email and password
 * 2. Click login button
 * 3. ViewModel validates inputs
 * 4. Repository calls API
 * 5. On success: Save token and navigate to dashboard
 * 6. On error: Show error dialog
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordButton: ImageButton
    private lateinit var loginButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emailErrorTextView: TextView
    private lateinit var passwordErrorTextView: TextView

    private lateinit var viewModel: LoginViewModel
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI components
        initializeViews()

        // Initialize ViewModel
        initializeViewModel()

        // Set up listeners
        setupListeners()

        // Set up observers
        observeViewModel()
    }

    /**
     * Initializes all UI components by finding their IDs.
     */
    private fun initializeViews() {
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        togglePasswordButton = findViewById(R.id.buttonTogglePassword)
        loginButton = findViewById(R.id.buttonLogin)
        loadingProgressBar = findViewById(R.id.progressBarLoading)
        emailErrorTextView = findViewById(R.id.textViewEmailError)
        passwordErrorTextView = findViewById(R.id.textViewPasswordError)
    }

    /**
     * Initializes ViewModel with AuthRepository.
     */
    private fun initializeViewModel() {
        val app = application as MainApplication
        val tokenManager = app.tokenManager
        val apiService = RetrofitClient.getApiService(tokenManager)
        val authRepository = AuthRepository(apiService, tokenManager)
        viewModel = LoginViewModel(authRepository)
    }

    /**
     * Sets up click listeners for UI components.
     */
    private fun setupListeners() {
        // Toggle password visibility
        togglePasswordButton.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            viewModel.togglePasswordVisibility()
        }

        // Clear errors when user types
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearErrors()
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearErrors()
        }

        // Login button click
        loginButton.setOnClickListener {
            performLogin()
        }
    }

    /**
     * Performs login with email and password from input fields.
     */
    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        viewModel.login(email, password)
    }

    /**
     * Observes ViewModel LiveData and updates UI accordingly.
     */
    private fun observeViewModel() {
        // Observe login result
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoadingState(true)
                }
                is NetworkResult.Success -> {
                    showLoadingState(false)
                    navigateToDashboard(result.data.user.role)
                }
                is NetworkResult.Error -> {
                    showLoadingState(false)
                    showErrorDialog(result.message)
                }
                is NetworkResult.Unauthorized -> {
                    showLoadingState(false)
                    showErrorDialog(Constants.ERROR_UNAUTHORIZED)
                }
            }
        }

        // Observe email error
        viewModel.emailError.observe(this) { error ->
            emailErrorTextView.text = error
            emailErrorTextView.visibility = if (error != null) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Observe password error
        viewModel.passwordError.observe(this) { error ->
            passwordErrorTextView.text = error
            passwordErrorTextView.visibility = if (error != null) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Observe password visibility
        viewModel.showPassword.observe(this) { isVisible ->
            updatePasswordFieldVisibility(isVisible)
        }
    }

    /**
     * Updates password field visibility.
     * @param isVisible true to show password, false to hide
     */
    private fun updatePasswordFieldVisibility(isVisible: Boolean) {
        val cursorPosition = passwordEditText.selectionStart
        if (isVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT
        } else {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passwordEditText.setSelection(cursorPosition)
    }

    /**
     * Shows or hides loading state.
     * @param isLoading true to show loading, false to hide
     */
    private fun showLoadingState(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        loginButton.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
    }

    /**
     * Shows error dialog with error message.
     * @param message Error message to display
     */
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Login Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Navigates to the appropriate dashboard based on user role.
     * @param userRole The role of the logged-in user (admin, staff, student)
     */
    private fun navigateToDashboard(userRole: String) {
        val intent = when (userRole) {
            Constants.ROLE_ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            Constants.ROLE_STAFF -> Intent(this, StaffDashboardActivity::class.java)
            Constants.ROLE_STUDENT -> Intent(this, StudentDashboardActivity::class.java)
            else -> {
                showErrorDialog("Unknown user role")
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

