package com.saroj.lmsmobile.ui.auth

import android.os.Bundle
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.repository.CompleteRegistrationRepository
import com.saroj.lmsmobile.ui.auth.viewmodel.CompleteRegistrationUiState
import com.saroj.lmsmobile.ui.auth.viewmodel.CompleteRegistrationViewModel

class CompleteRegistrationActivity : AppCompatActivity() {

    private lateinit var roleInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var identifierInputLayout: TextInputLayout
    private lateinit var phoneInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var roleFloatingLabel: TextView
    private lateinit var emailFloatingLabel: TextView
    private lateinit var identifierFloatingLabel: TextView
    private lateinit var phoneFloatingLabel: TextView
    private lateinit var passwordFloatingLabel: TextView
    private lateinit var confirmPasswordFloatingLabel: TextView

    private lateinit var roleDropdown: AutoCompleteTextView
    private lateinit var emailEditText: EditText
    private lateinit var identifierEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText

    private lateinit var completeButton: FrameLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var completeButtonIcon: ImageView
    private lateinit var completeButtonText: TextView
    private lateinit var registrationScrollView: ScrollView

    private lateinit var viewModel: CompleteRegistrationViewModel

    private var selectedRole = ROLE_STUDENT
    private var keyboardHeightPx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_registration)

        initializeViews()
        initializeViewModel()
        setupRoleDropdown()
        setupListeners()
        observeViewModel()
        playEntranceAnimation()
    }

    private fun initializeViews() {
        registrationScrollView = findViewById(R.id.registrationScrollView)

        roleInputLayout = findViewById(R.id.textInputLayoutRole)
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        identifierInputLayout = findViewById(R.id.textInputLayoutIdentifier)
        phoneInputLayout = findViewById(R.id.textInputLayoutPhone)
        passwordInputLayout = findViewById(R.id.textInputLayoutPassword)
        confirmPasswordInputLayout = findViewById(R.id.textInputLayoutConfirmPassword)
        roleFloatingLabel = findViewById(R.id.floatingLabelRole)
        emailFloatingLabel = findViewById(R.id.floatingLabelEmail)
        identifierFloatingLabel = findViewById(R.id.floatingLabelIdentifier)
        phoneFloatingLabel = findViewById(R.id.floatingLabelPhone)
        passwordFloatingLabel = findViewById(R.id.floatingLabelPassword)
        confirmPasswordFloatingLabel = findViewById(R.id.floatingLabelConfirmPassword)

        roleDropdown = findViewById(R.id.autoCompleteRole)
        emailEditText = findViewById(R.id.editTextEmail)
        identifierEditText = findViewById(R.id.editTextIdentifier)
        phoneEditText = findViewById(R.id.editTextPhone)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)

        completeButton = findViewById(R.id.buttonCompleteRegistration)
        loadingProgressBar = findViewById(R.id.progressBarLoading)
        completeButtonIcon = findViewById(R.id.imageViewLoginIcon)
        completeButtonText = findViewById(R.id.textViewLoginButtonText)

        completeButtonIcon.setImageResource(R.drawable.ic_person_add_white)
        completeButtonText.text = "Complete Registration"
        completeButton.contentDescription = "Complete Registration"
        updatePasswordRequirements()
    }

    private fun initializeViewModel() {
        val app = application as MainApplication
        val apiService = RetrofitClient.getApiService(app.tokenManager)
        viewModel = CompleteRegistrationViewModel(CompleteRegistrationRepository(apiService))
    }

    private fun setupRoleDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            listOf(DISPLAY_STUDENT, DISPLAY_STAFF)
        )
        roleDropdown.setAdapter(adapter)
        roleDropdown.setText(DISPLAY_STUDENT, false)
        updateIdentifierLabel()
        updateAllFloatingLabels(animate = false)

        roleDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedRole = if (position == 1) ROLE_STAFF else ROLE_STUDENT
            identifierEditText.text?.clear()
            viewModel.clearIdentifierError()
            updateIdentifierLabel()
            updateAllFloatingLabels()
        }

        roleDropdown.setOnClickListener {
            roleDropdown.showDropDown()
        }
    }

    private fun setupListeners() {
        setupKeyboardAwareScrolling()

        completeButton.setOnClickListener {
            submitRegistration()
        }

        findViewById<TextView>(R.id.textViewSignIn).setOnClickListener {
            finish()
        }

        roleDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearFieldError("role")
                scrollFocusedViewIntoView(roleInputLayout)
            }
            updateFloatingLabel(roleDropdown, roleFloatingLabel, "Role")
        }
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearFieldError("email")
                scrollFocusedViewIntoView(emailInputLayout)
            }
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }
        identifierEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearIdentifierError()
                scrollFocusedViewIntoView(identifierInputLayout)
            }
            updateFloatingLabel(identifierEditText, identifierFloatingLabel, identifierLabel())
        }
        phoneEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearFieldError("phone")
                scrollFocusedViewIntoView(phoneInputLayout)
            }
            updateFloatingLabel(phoneEditText, phoneFloatingLabel, "Phone number")
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearFieldError("password")
                scrollFocusedViewIntoView(passwordInputLayout)
            }
            updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password")
        }
        confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearFieldError("password_confirmation")
                scrollFocusedViewIntoView(confirmPasswordInputLayout)
            }
            updateFloatingLabel(confirmPasswordEditText, confirmPasswordFloatingLabel, "Confirm password")
        }

        roleDropdown.doOnTextChanged { _, _, _, _ ->
            viewModel.clearFieldError("role")
            updateFloatingLabel(roleDropdown, roleFloatingLabel, "Role")
        }
        emailEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearFieldError("email")
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }
        identifierEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearIdentifierError()
            updateFloatingLabel(identifierEditText, identifierFloatingLabel, identifierLabel())
        }
        phoneEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearFieldError("phone")
            updateFloatingLabel(phoneEditText, phoneFloatingLabel, "Phone number")
        }
        passwordEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearFieldError("password")
            updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password")
            updatePasswordRequirements()
        }
        confirmPasswordEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearFieldError("password_confirmation")
            updateFloatingLabel(confirmPasswordEditText, confirmPasswordFloatingLabel, "Confirm password")
            updatePasswordRequirements()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                CompleteRegistrationUiState.Idle -> showLoadingState(false)
                CompleteRegistrationUiState.Loading -> showLoadingState(true)
                is CompleteRegistrationUiState.Success -> {
                    showLoadingState(false)
                    clearForm()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is CompleteRegistrationUiState.Error -> {
                    showLoadingState(false)
                    if (state.message.isNotBlank()) {
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.fieldErrors.observe(this) { errors ->
            renderFieldErrors(errors)
        }
    }

    private fun submitRegistration() {
        viewModel.completeRegistration(
            role = selectedRole,
            email = emailEditText.text.toString(),
            identifier = identifierEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            password = passwordEditText.text.toString(),
            confirmPassword = confirmPasswordEditText.text.toString()
        )
    }

    private fun updateIdentifierLabel() {
        val label = identifierLabel()
        identifierFloatingLabel.text = label
        identifierEditText.hint = label
            updateFloatingLabel(identifierEditText, identifierFloatingLabel, label)
    }

    private fun identifierLabel(): String {
        return if (selectedRole == ROLE_STAFF) "Staff ID" else "Student ID"
    }

    private fun renderFieldErrors(errors: Map<String, String>) {
        setError(roleInputLayout, errors["role"])
        setError(emailInputLayout, errors["email"])
        setError(
            identifierInputLayout,
            errors["identifier"] ?: errors["student_id"] ?: errors["staff_id"]
        )
        setError(phoneInputLayout, errors["phone"])
        setError(passwordInputLayout, errors["password"])
        setError(confirmPasswordInputLayout, errors["password_confirmation"])
    }

    private fun setError(inputLayout: TextInputLayout, error: String?) {
        inputLayout.isErrorEnabled = !error.isNullOrBlank()
        inputLayout.error = error
    }

    private fun showLoadingState(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        completeButtonIcon.visibility = if (isLoading) View.GONE else View.VISIBLE
        completeButtonText.text = if (isLoading) "Completing..." else "Complete Registration"
        completeButton.contentDescription = if (isLoading) "Completing registration" else "Complete Registration"
        completeButton.isEnabled = !isLoading

        listOf(roleDropdown, emailEditText, identifierEditText, phoneEditText, passwordEditText, confirmPasswordEditText)
            .forEach { it.isEnabled = !isLoading }

        listOf(
            roleInputLayout,
            emailInputLayout,
            identifierInputLayout,
            phoneInputLayout,
            passwordInputLayout,
            confirmPasswordInputLayout
        ).forEach { it.isEnabled = !isLoading }
    }

    private fun updatePasswordRequirements() {
        val password = passwordEditText.text?.toString().orEmpty()
        val confirmPassword = confirmPasswordEditText.text?.toString().orEmpty()

        updateRequirement(R.id.textReqLength, "At least 8 characters", password.length >= 8)
        updateRequirement(R.id.textReqUppercase, "At least one uppercase letter", password.any { it.isUpperCase() })
        updateRequirement(R.id.textReqLowercase, "At least one lowercase letter", password.any { it.isLowerCase() })
        updateRequirement(R.id.textReqNumber, "At least one number", password.any { it.isDigit() })
        updateRequirement(
            R.id.textReqSymbol,
            "At least one symbol",
            password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        )
        updateRequirement(
            R.id.textReqMatch,
            "Passwords must match",
            password.isNotBlank() && password == confirmPassword
        )
    }

    private fun updateRequirement(textId: Int, label: String, met: Boolean) {
        findViewById<TextView>(textId)?.apply {
            text = "${if (met) "OK" else "--"} $label"
            setTextColor(
                ContextCompat.getColor(
                    this@CompleteRegistrationActivity,
                    if (met) R.color.profile_green else R.color.profile_text_muted
                )
            )
        }
    }

    private fun updateAllFloatingLabels(animate: Boolean = true) {
        updateFloatingLabel(roleDropdown, roleFloatingLabel, "Role", animate)
        updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address", animate)
        updateFloatingLabel(identifierEditText, identifierFloatingLabel, identifierLabel(), animate)
        updateFloatingLabel(phoneEditText, phoneFloatingLabel, "Phone number", animate)
        updateFloatingLabel(passwordEditText, passwordFloatingLabel, "Password", animate)
        updateFloatingLabel(confirmPasswordEditText, confirmPasswordFloatingLabel, "Confirm password", animate)
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

    private fun setupKeyboardAwareScrolling() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val visibleFrame = Rect()
                rootView.getWindowVisibleDisplayFrame(visibleFrame)
                val height = rootView.rootView.height
                val newKeyboardHeight = (height - visibleFrame.bottom).coerceAtLeast(0)
                val keyboardVisible = newKeyboardHeight > height * 0.15f
                keyboardHeightPx = if (keyboardVisible) newKeyboardHeight else 0

                val bottomPadding = if (keyboardVisible) {
                    keyboardHeightPx + (32 * resources.displayMetrics.density).toInt()
                } else {
                    (32 * resources.displayMetrics.density).toInt()
                }

                if (registrationScrollView.paddingBottom != bottomPadding) {
                    registrationScrollView.setPadding(
                        registrationScrollView.paddingLeft,
                        registrationScrollView.paddingTop,
                        registrationScrollView.paddingRight,
                        bottomPadding
                    )
                }

                if (keyboardVisible) {
                    currentFocus?.let { focusedView ->
                        val target = fieldContainerFor(focusedView) ?: focusedView
                        scrollFocusedViewIntoView(target)
                    }
                }
            }
        })
    }

    private fun fieldContainerFor(view: View): View? {
        return when (view.id) {
            R.id.autoCompleteRole -> roleInputLayout
            R.id.editTextEmail -> emailInputLayout
            R.id.editTextIdentifier -> identifierInputLayout
            R.id.editTextPhone -> phoneInputLayout
            R.id.editTextPassword -> passwordInputLayout
            R.id.editTextConfirmPassword -> confirmPasswordInputLayout
            else -> null
        }
    }

    private fun scrollFocusedViewIntoView(view: View) {
        registrationScrollView.postDelayed({
            val rect = Rect()
            view.getDrawingRect(rect)
            registrationScrollView.offsetDescendantRectToMyCoords(view, rect)
            val extraSpace = (96 * resources.displayMetrics.density).toInt()
            val visibleHeight = (registrationScrollView.height - keyboardHeightPx).coerceAtLeast(
                (180 * resources.displayMetrics.density).toInt()
            )
            val targetY = (rect.bottom - visibleHeight + extraSpace).coerceAtLeast(0)
            registrationScrollView.smoothScrollTo(0, targetY)
        }, 260L)

        registrationScrollView.postDelayed({
            val rect = Rect()
            view.getDrawingRect(rect)
            registrationScrollView.offsetDescendantRectToMyCoords(view, rect)
            val extraSpace = (96 * resources.displayMetrics.density).toInt()
            val visibleHeight = (registrationScrollView.height - keyboardHeightPx).coerceAtLeast(
                (180 * resources.displayMetrics.density).toInt()
            )
            val targetY = (rect.bottom - visibleHeight + extraSpace).coerceAtLeast(0)
            registrationScrollView.smoothScrollTo(0, targetY)
        }, 520L)
    }

    private fun clearForm() {
        selectedRole = ROLE_STUDENT
        roleDropdown.setText(DISPLAY_STUDENT, false)
        updateIdentifierLabel()
        emailEditText.text?.clear()
        identifierEditText.text?.clear()
        phoneEditText.text?.clear()
        passwordEditText.text?.clear()
        confirmPasswordEditText.text?.clear()
        updatePasswordRequirements()
        updateAllFloatingLabels(animate = false)
        viewModel.clearAllErrors()
    }

    private fun playEntranceAnimation() {
        val brandHeader = findViewById<View>(R.id.authBrandHeader)
        val registrationCard = findViewById<View>(R.id.registrationCard)

        listOf(brandHeader, registrationCard).forEach { view ->
            view.alpha = 0f
            view.translationY = 28f
        }

        brandHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        registrationCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(110L)
            .setDuration(460L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private companion object {
        const val ROLE_STUDENT = "student"
        const val ROLE_STAFF = "staff"
        const val DISPLAY_STUDENT = "Student"
        const val DISPLAY_STAFF = "Staff"
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
