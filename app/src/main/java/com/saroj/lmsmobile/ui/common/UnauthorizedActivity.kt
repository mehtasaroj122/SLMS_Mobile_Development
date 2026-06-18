package com.saroj.lmsmobile.ui.common

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.ui.auth.LoginActivity
import kotlinx.coroutines.launch

/**
 * UnauthorizedActivity is displayed when user's session expires (401 response).
 *
 * Features:
 * - Shows message that session has expired
 * - Provides button to return to login
 * - Automatically clears user data
 *
 * Usage:
 * When API returns 401 Unauthorized:
 * 1. AuthInterceptor detects 401
 * 2. Or in Repository: emit NetworkResult.Unauthorized()
 * 3. Activity/Fragment catches this and navigates to UnauthorizedActivity
 * 4. User clicks "Login Again" button
 * 5. Redirected to LoginActivity
 */
class UnauthorizedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unauthorized)

        // Clear user data
        val app = application as MainApplication
        lifecycleScope.launch {
            app.tokenManager.clearAllData()
        }

        // Set up login button
        val loginButton = findViewById<Button>(R.id.buttonLoginAgain)
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        // Prevent going back
        // Force user to login again
    }
}

