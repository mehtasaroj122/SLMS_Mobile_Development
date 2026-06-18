package com.saroj.lmsmobile

import android.app.Application
import com.saroj.lmsmobile.storage.TokenManager

/**
 * MainApplication extends Application to provide application-wide initialization.
 *
 * Responsibilities:
 * - Initialize TokenManager for DataStore access
 * - Provide singleton instances to all activities/fragments
 * - Set up any app-level configurations
 *
 * Usage:
 *   val tokenManager = (application as MainApplication).tokenManager
 *
 * Lifecycle:
 * - Created when app starts
 * - Exists for entire app lifetime
 * - Destroyed when app is completely closed
 *
 * Note:
 * This is declared in AndroidManifest.xml:
 * android:name=".MainApplication"
 */
class MainApplication : Application() {

    // Lazy initialization of TokenManager
    // TokenManager is initialized only when first accessed
    val tokenManager: TokenManager by lazy {
        TokenManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize app-level dependencies here
        // TokenManager will be lazily initialized when first accessed
    }
}

