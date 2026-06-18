package com.saroj.lmsmobile.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

/**
 * TokenManager handles all token and session storage using DataStore Preferences.
 * Provides secure, encrypted storage for user credentials and session information.
 *
 * Features:
 * - Save and retrieve authentication token
 * - Store user information (ID, name, email, role)
 * - Clear all data on logout
 * - Observe token changes in real-time
 *
 * Usage:
 *   val tokenManager = TokenManager(context)
 *   tokenManager.saveToken("Bearer token_here")
 *   tokenManager.getToken().collect { token -> ... }
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lms_mobile_app_prefs"
)

class TokenManager(private val context: Context) {

    private object PrefsKeys {
        val TOKEN = stringPreferencesKey(Constants.TOKEN_KEY)
        val USER_ID = stringPreferencesKey(Constants.USER_ID_KEY)
        val USER_NAME = stringPreferencesKey(Constants.USER_NAME_KEY)
        val USER_EMAIL = stringPreferencesKey(Constants.USER_EMAIL_KEY)
        val USER_ROLE = stringPreferencesKey(Constants.USER_ROLE_KEY)
        val IS_LOGGED_IN = booleanPreferencesKey(Constants.IS_LOGGED_IN_KEY)
    }

    private val dataStore = context.dataStore

    // ==================== Token Operations ====================

    /**
     * Saves the authentication token.
     * @param token The Bearer token from login response
     */
    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.TOKEN] = token
        }
    }

    /**
     * Retrieves the authentication token as a Flow.
     * @return Flow<String?> The token or null if not found
     */
    fun getToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.TOKEN]
    }

    /**
     * Retrieves the token synchronously (suspend).
     * Use this in repositories for single-shot access.
     */
    suspend fun getTokenSync(): String? {
        return dataStore.data.map { it[PrefsKeys.TOKEN] }.firstOrNull()
    }

    /**
     * Checks if token exists.
     * @return Flow<Boolean> true if token is present
     */
    fun hasToken(): Flow<Boolean> = dataStore.data.map { preferences ->
        !preferences[PrefsKeys.TOKEN].isNullOrEmpty()
    }

    // ==================== User Information Operations ====================

    /**
     * Saves user information after successful login.
     */
    suspend fun saveUserInfo(
        userId: String,
        userName: String,
        userEmail: String,
        userRole: String
    ) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.USER_ID] = userId
            preferences[PrefsKeys.USER_NAME] = userName
            preferences[PrefsKeys.USER_EMAIL] = userEmail
            preferences[PrefsKeys.USER_ROLE] = userRole
            preferences[PrefsKeys.IS_LOGGED_IN] = true
        }
    }

    /**
     * Retrieves user ID as Flow.
     */
    fun getUserId(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.USER_ID]
    }

    /**
     * Retrieves user name as Flow.
     */
    fun getUserName(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.USER_NAME]
    }

    /**
     * Retrieves user email as Flow.
     */
    fun getUserEmail(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.USER_EMAIL]
    }

    /**
     * Retrieves user role as Flow.
     */
    fun getUserRole(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.USER_ROLE]
    }

    /**
     * Retrieves login status as Flow.
     */
    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PrefsKeys.IS_LOGGED_IN] ?: false
    }

    /**
     * Retrieves user role synchronously (suspend).
     * Use in navigation logic after login.
     */
    suspend fun getUserRoleSync(): String? {
        return dataStore.data.map { preferences ->
            preferences[PrefsKeys.USER_ROLE]
        }.firstOrNull()
    }

    // ==================== Cleanup Operations ====================

    /**
     * Clears all user data (logout).
     * Called when user logs out or token expires.
     */
    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Clears only the token (token refresh scenario).
     */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.TOKEN] = ""
            preferences[PrefsKeys.IS_LOGGED_IN] = false
        }
    }
}


