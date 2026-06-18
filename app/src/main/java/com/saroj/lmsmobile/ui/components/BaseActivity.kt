package com.saroj.lmsmobile.ui.components

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.saroj.lmsmobile.R
import com.saroj.lmsmobile.MainApplication
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.ui.auth.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * BaseActivity is a base class for all Activities in the application.
 *
 * Provides:
 * - TokenManager access for all activities
 * - Logout functionality
 * - Coroutine scope for launching background tasks
 * - Consistent error handling
 * - Common menu initialization
 *
 * Usage:
 *   class MyActivity : BaseActivity() {
 *       override fun onCreate(...) {
 *           super.onCreate(...)
 *           // Use tokenManager here
 *       }
 *   }
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var tokenManager: TokenManager
    protected val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize TokenManager for child classes
        tokenManager = (application as MainApplication).tokenManager
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                logout()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Logs out the user and returns to login screen.
     * Clears all stored user data and token.
     */
    protected fun logout() {
        activityScope.launch {
            tokenManager.clearAllData()
            val intent = Intent(this@BaseActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}


