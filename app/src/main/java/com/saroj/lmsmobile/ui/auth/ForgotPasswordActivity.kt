package com.saroj.lmsmobile.ui.auth

import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.api.RetrofitClient
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.AuthRepository
import com.saroj.lmsmobile.ui.auth.viewmodel.ForgotPasswordViewModel

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var forgotPasswordScrollView: ScrollView
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailFloatingLabel: TextView
    private lateinit var emailEditText: EditText
    private lateinit var sendButton: FrameLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var sendButtonIcon: ImageView
    private lateinit var sendButtonText: TextView
    private lateinit var statusMessageLayout: LinearLayout
    private lateinit var statusMessageIcon: ImageView
    private lateinit var statusMessageText: TextView
    private lateinit var viewModel: ForgotPasswordViewModel

    private var keyboardHeightPx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initializeViews()
        initializeViewModel()
        setupListeners()
        observeViewModel()
        playEntranceAnimation()
    }

    private fun initializeViews() {
        forgotPasswordScrollView = findViewById(R.id.forgotPasswordScrollView)
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        emailFloatingLabel = findViewById(R.id.floatingLabelEmail)
        emailEditText = findViewById(R.id.editTextEmail)
        sendButton = findViewById(R.id.buttonForgotPassword)
        loadingProgressBar = findViewById(R.id.progressBarLoading)
        sendButtonIcon = findViewById(R.id.imageViewLoginIcon)
        sendButtonText = findViewById(R.id.textViewLoginButtonText)
        statusMessageLayout = findViewById(R.id.layoutStatusMessage)
        statusMessageIcon = findViewById(R.id.imageViewStatusIcon)
        statusMessageText = findViewById(R.id.textViewStatusMessage)

        emailInputLayout.isHintEnabled = false
        sendButtonIcon.setImageResource(R.drawable.ic_email)
        sendButtonIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
        sendButtonText.text = BUTTON_TEXT
        sendButton.contentDescription = BUTTON_TEXT
        updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address", animate = false)
    }

    private fun initializeViewModel() {
        val app = application as MainApplication
        val apiService = RetrofitClient.getApiService(app.tokenManager)
        val authRepository = AuthRepository(apiService, app.tokenManager)
        viewModel = ForgotPasswordViewModel(authRepository)
    }

    private fun setupListeners() {
        setupKeyboardAwareScrolling()

        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.clearErrors()
                hideStatusMessage()
                scrollFocusedViewIntoView(emailInputLayout)
            }
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }

        emailEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearErrors()
            hideStatusMessage()
            updateFloatingLabel(emailEditText, emailFloatingLabel, "Email address")
        }

        sendButton.setOnClickListener {
            viewModel.sendResetLink(emailEditText.text.toString())
        }

        findViewById<View>(R.id.layoutBackToLogin).setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.forgotPasswordResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoadingState(true)
                is NetworkResult.Success -> {
                    showLoadingState(false)
                    emailInputLayout.isErrorEnabled = false
                    emailInputLayout.error = null
                    showStatusMessage(
                        message = result.data.message?.takeIf { it.isNotBlank() }
                            ?: "We have emailed your password reset link.",
                        isSuccess = true
                    )
                }
                is NetworkResult.Error -> {
                    showLoadingState(false)
                    val message = result.message.ifBlank {
                        "We can't find a user with that email address."
                    }
                    emailInputLayout.isErrorEnabled = true
                    emailInputLayout.error = message
                    showStatusMessage(message, isSuccess = false)
                }
                is NetworkResult.Unauthorized -> {
                    showLoadingState(false)
                    val message = "We can't find a user with that email address."
                    emailInputLayout.isErrorEnabled = true
                    emailInputLayout.error = message
                    showStatusMessage(message, isSuccess = false)
                }
            }
        }

        viewModel.emailError.observe(this) { error ->
            emailInputLayout.isErrorEnabled = !error.isNullOrBlank()
            emailInputLayout.error = error
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        sendButtonIcon.visibility = if (isLoading) View.GONE else View.VISIBLE
        sendButtonText.text = if (isLoading) "Sending..." else BUTTON_TEXT
        sendButton.contentDescription = if (isLoading) "Sending password reset link" else BUTTON_TEXT
        sendButton.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        emailInputLayout.isEnabled = !isLoading
    }

    private fun showStatusMessage(message: String, isSuccess: Boolean) {
        statusMessageLayout.visibility = View.VISIBLE
        statusMessageLayout.setBackgroundResource(
            if (isSuccess) R.drawable.bg_auth_success_message else R.drawable.bg_auth_error_message
        )
        statusMessageIcon.setImageResource(
            if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_warning
        )
        statusMessageIcon.setColorFilter(
            ContextCompat.getColor(
                this,
                if (isSuccess) R.color.profile_green else R.color.profile_red
            )
        )
        statusMessageText.text = message
        statusMessageText.setTextColor(
            ContextCompat.getColor(
                this,
                if (isSuccess) R.color.profile_green else R.color.profile_red
            )
        )
    }

    private fun hideStatusMessage() {
        statusMessageLayout.visibility = View.GONE
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

                if (forgotPasswordScrollView.paddingBottom != bottomPadding) {
                    forgotPasswordScrollView.setPadding(
                        forgotPasswordScrollView.paddingLeft,
                        forgotPasswordScrollView.paddingTop,
                        forgotPasswordScrollView.paddingRight,
                        bottomPadding
                    )
                }

                if (keyboardVisible && currentFocus === emailEditText) {
                    scrollFocusedViewIntoView(emailInputLayout)
                }
            }
        })
    }

    private fun scrollFocusedViewIntoView(view: View) {
        forgotPasswordScrollView.postDelayed({
            val rect = Rect()
            view.getDrawingRect(rect)
            forgotPasswordScrollView.offsetDescendantRectToMyCoords(view, rect)
            val extraSpace = (96 * resources.displayMetrics.density).toInt()
            val visibleHeight = (forgotPasswordScrollView.height - keyboardHeightPx).coerceAtLeast(
                (180 * resources.displayMetrics.density).toInt()
            )
            val targetY = (rect.bottom - visibleHeight + extraSpace).coerceAtLeast(0)
            forgotPasswordScrollView.smoothScrollTo(0, targetY)
        }, 260L)
    }

    private fun playEntranceAnimation() {
        val brandHeader = findViewById<View>(R.id.authBrandHeader)
        val forgotPasswordCard = findViewById<View>(R.id.forgotPasswordCard)

        listOf(brandHeader, forgotPasswordCard).forEach { view ->
            view.alpha = 0f
            view.translationY = 28f
        }

        brandHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        forgotPasswordCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(110L)
            .setDuration(460L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private companion object {
        const val BUTTON_TEXT = "Email Password Reset Link"
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
