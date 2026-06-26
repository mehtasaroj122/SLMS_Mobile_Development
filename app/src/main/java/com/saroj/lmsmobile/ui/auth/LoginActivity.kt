package com.saroj.lmsmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
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

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailFloatingLabel: TextView
    private lateinit var passwordFloatingLabel: TextView
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: View
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loginButtonIcon: ImageView
    private lateinit var loginButtonText: TextView

    private lateinit var viewModel: LoginViewModel

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

        // Polish first paint without delaying interaction
        playEntranceAnimation()
    }

    /**
     * Initializes all UI components by finding their IDs.
     */
    private fun initializeViews() {
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        passwordInputLayout = findViewById(R.id.textInputLayoutPassword)
        emailFloatingLabel = findViewById(R.id.floatingLabelEmail)
        passwordFloatingLabel = findViewById(R.id.floatingLabelPassword)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        loadingProgressBar = findViewById(R.id.progressBarLoading)
        loginButtonIcon = findViewById(R.id.imageViewLoginIcon)
        loginButtonText = findViewById(R.id.textViewLoginButtonText)

        emailInputLayout.isHintEnabled = false
        passwordInputLayout.isHintEnabled = false
        updateAllFloatingLabels(animate = false)
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
        // Clear errors when user focuses or edits a field
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearErrors()
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.clearErrors()
            updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password")
        }
        emailEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearErrors()
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }
        passwordEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearErrors()
            updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password")
        }

        // Login button click
        loginButton.setOnClickListener {
            performLogin()
        }

        findViewById<TextView>(R.id.textViewForgotPassword).setOnClickListener {
            Toast.makeText(this, "Forgot password is not available yet", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.textViewSignUp).setOnClickListener {
            startActivity(Intent(this, CompleteRegistrationActivity::class.java))
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
                    android.util.Log.d("LoginActivity", "Login successful, role: ${result.data.user.role}")
                    navigateToDashboard(result.data.user.role)
                }
                is NetworkResult.Error -> {
                    showLoadingState(false)
                    android.util.Log.e("LoginActivity", "Login error: ${result.message}")
                    showErrorDialog(result.message)
                }
                is NetworkResult.Unauthorized -> {
                    showLoadingState(false)
                    android.util.Log.e("LoginActivity", "Login unauthorized - session expired")
                    showErrorDialog(Constants.ERROR_UNAUTHORIZED)
                }
            }
        }

        // Observe email error
        viewModel.emailError.observe(this) { error ->
            emailInputLayout.isErrorEnabled = error != null
            emailInputLayout.error = error
        }

        // Observe password error
        viewModel.passwordError.observe(this) { error ->
            passwordInputLayout.isErrorEnabled = error != null
            passwordInputLayout.error = error
        }
    }

    /**
     * Shows or hides loading state.
     * @param isLoading true to show loading, false to hide
     */
    private fun showLoadingState(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButtonIcon.visibility = if (isLoading) View.GONE else View.VISIBLE
        loginButtonText.text = if (isLoading) "Logging in..." else "Login"
        loginButton.contentDescription = if (isLoading) "Logging in" else "Login"
        loginButton.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        emailInputLayout.isEnabled = !isLoading
        passwordInputLayout.isEnabled = !isLoading
    }

    private fun updateAllFloatingLabels(animate: Boolean = true) {
        updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address", animate)
        updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password", animate)
    }

    private fun updateFloatingLabel(
        editText: EditText,
        floatingLabel: TextView,
        label: String,
        animate: Boolean = true
    ) {
        val shouldFloat = editText.hasFocus() || editText.text?.isNotBlank() == true
        val stateChanged = floatingLabel.isSelected != shouldFloat
        floatingLabel.text = label
        floatingLabel.visibility = View.VISIBLE
        floatingLabel.isSelected = shouldFloat
        editText.hint = null

        val targetTranslationY = if (shouldFloat) LABEL_FLOAT_Y_DP.dp else LABEL_INSIDE_Y_DP.dp
        val targetTranslationX = if (shouldFloat) LABEL_FLOAT_X_DP.dp else LABEL_INSIDE_X_DP.dp
        val targetScale = if (shouldFloat) LABEL_FLOAT_SCALE else LABEL_INSIDE_SCALE
        val targetColor = ContextCompat.getColor(
            this,
            if (shouldFloat) R.color.auth_secondary else R.color.auth_text_hint
        )
        val targetBackground = if (shouldFloat) R.color.auth_card else R.color.transparent

        floatingLabel.setBackgroundColor(ContextCompat.getColor(this, targetBackground))
        floatingLabel.setTextColor(targetColor)

        if (!animate || !stateChanged) {
            floatingLabel.animate().cancel()
            floatingLabel.translationY = targetTranslationY
            floatingLabel.translationX = targetTranslationX
            floatingLabel.scaleX = targetScale
            floatingLabel.scaleY = targetScale
            return
        }

        floatingLabel.animate()
            .translationY(targetTranslationY)
            .translationX(targetTranslationX)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(180L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun playEntranceAnimation() {
        val brandHeader = findViewById<View>(R.id.authBrandHeader)
        val loginCard = findViewById<View>(R.id.loginCard)

        listOf(brandHeader, loginCard).forEach { view ->
            view.alpha = 0f
            view.translationY = 28f
        }

        brandHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        loginCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(110L)
            .setDuration(460L)
            .setInterpolator(DecelerateInterpolator())
            .start()
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
        val normalizedRole = Constants.normalizeRole(userRole)
        android.util.Log.d("LoginActivity", "Navigating to dashboard for role: $userRole normalized as: $normalizedRole")
        val intent = when (normalizedRole) {
            Constants.ROLE_ADMIN -> {
                android.util.Log.d("LoginActivity", "Target activity: AdminDashboardActivity")
                Intent(this, AdminDashboardActivity::class.java)
            }
            Constants.ROLE_STAFF -> {
                android.util.Log.d("LoginActivity", "Target activity: StaffDashboardActivity")
                Intent(this, StaffDashboardActivity::class.java)
            }
            Constants.ROLE_STUDENT -> {
                android.util.Log.d("LoginActivity", "Target activity: StudentDashboardActivity")
                Intent(this, StudentDashboardActivity::class.java)
            }
            else -> {
                android.util.Log.e("LoginActivity", "Unknown user role: $userRole")
                showErrorDialog("Unknown user role: $userRole")
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private companion object {
        const val LABEL_INSIDE_Y_DP = 35f
        const val LABEL_FLOAT_Y_DP = 2f
        const val LABEL_INSIDE_X_DP = 18f
        const val LABEL_FLOAT_X_DP = 0f
        const val LABEL_INSIDE_SCALE = 1f
        const val LABEL_FLOAT_SCALE = 0.9f
    }

    private val Float.dp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            resources.displayMetrics
        )
}

