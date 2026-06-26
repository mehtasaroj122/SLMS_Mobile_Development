package com.saroj.lmsmobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.admin.AdminDashboardActivity
import com.saroj.lmsmobile.ui.staff.StaffDashboardActivity
import com.saroj.lmsmobile.ui.student.StudentDashboardActivity
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * SplashActivity is the entry point of the application.
 *
 * Responsibilities:
 * - Display splash screen for 2 seconds
 * - Check if user has valid token (is logged in)
 * - Fetch user role from storage
 * - Navigate to appropriate dashboard based on role:
 *   - Admin → AdminDashboardActivity
 *   - Staff → StaffDashboardActivity
 *   - Student → StudentDashboardActivity
 *   - No token → LoginActivity
 *
 * Navigation Flow:
 * SplashActivity
 * ├─ Has token? + Role found?
 * │  ├─ Role = admin → AdminDashboardActivity
 * │  ├─ Role = staff → StaffDashboardActivity
 * │  ├─ Role = student → StudentDashboardActivity
 * │  └─ Unknown role → LoginActivity
 * └─ No token → LoginActivity
 *
 * The splash screen is shown for SPLASH_SCREEN_DURATION (2 seconds) to
 * give a polished feel and allow async operations to complete.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private val splashScreenDuration = Constants.SPLASH_SCREEN_DURATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        playSplashAnimation()

        // Initialize TokenManager from Application context
        tokenManager = (application as MainApplication).tokenManager

        // Schedule navigation after splash screen duration
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, splashScreenDuration)
    }

    /**
     * Checks if user is authenticated and navigates to appropriate screen.
     *
     * Flow:
     * 1. Get token from DataStore
     * 2. If token exists:
     *    a. Get user role
     *    b. Navigate to role-specific dashboard
     * 3. If no token:
     *    a. Navigate to LoginActivity
     */
    private fun checkAuthenticationAndNavigate() {
        lifecycleScope.launch {
            try {
                // Check if token exists
                val token = tokenManager.getToken().firstOrNull()

                if (token.isNullOrEmpty()) {
                    // No token, redirect to login
                    navigateToLogin()
                } else {
                    // Token exists, get user role and navigate to dashboard
                    val userRole = Constants.normalizeRole(tokenManager.getUserRole().firstOrNull())

                    when (userRole) {
                        Constants.ROLE_ADMIN -> navigateToAdminDashboard()
                        Constants.ROLE_STAFF -> navigateToStaffDashboard()
                        Constants.ROLE_STUDENT -> navigateToStudentDashboard()
                        else -> {
                            // Unknown role, redirect to login for safety
                            tokenManager.clearAllData()
                            navigateToLogin()
                        }
                    }
                }
            } catch (e: Exception) {
                // On any error, redirect to login
                navigateToLogin()
            }

            // Finish splash activity so user can't go back to it
            finish()
        }
    }

    /**
     * Navigates to LoginActivity.
     */
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Navigates to AdminDashboardActivity.
     */
    private fun navigateToAdminDashboard() {
        startActivity(Intent(this, AdminDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Navigates to StaffDashboardActivity.
     */
    private fun navigateToStaffDashboard() {
        startActivity(Intent(this, StaffDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Navigates to StudentDashboardActivity.
     */
    private fun navigateToStudentDashboard() {
        startActivity(Intent(this, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun playSplashAnimation() {
        val logoCard = findViewById<View>(R.id.splashLogoCard)
        val title = findViewById<View>(R.id.textViewSplashTitle)
        val subtitle = findViewById<View>(R.id.textViewSplashSubtitle)
        val progress = findViewById<View>(R.id.progressBarSplash)
        val interpolator = DecelerateInterpolator()

        logoCard.alpha = 0f
        logoCard.scaleX = 0.9f
        logoCard.scaleY = 0.9f

        listOf(title, subtitle, progress).forEach { view ->
            view.alpha = 0f
            view.translationY = 18f
        }

        logoCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(520L)
            .setInterpolator(interpolator)
            .start()

        listOf(title, subtitle, progress).forEachIndexed { index, view ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(180L + index * 90L)
                .setDuration(420L)
                .setInterpolator(interpolator)
                .start()
        }
    }
}

