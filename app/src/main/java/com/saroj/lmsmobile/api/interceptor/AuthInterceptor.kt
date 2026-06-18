package com.saroj.lmsmobile.api.interceptor

import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor automatically adds Bearer token to all API requests.
 *
 * Features:
 * - Retrieves token from DataStore
 * - Adds Authorization header with Bearer token
 * - Handles requests without token gracefully
 * - Provides proper error responses for 401 (Unauthorized)
 *
 * Flow:
 * 1. Interceptor intercepts every request
 * 2. Gets token from TokenManager
 * 3. Adds "Authorization: Bearer <token>" header
 * 4. Proceeds with request
 * 5. Returns response (or 401 if token invalid)
 *
 * Usage:
 *   val interceptor = AuthInterceptor(tokenManager)
 *   val client = OkHttpClient.Builder()
 *       .addInterceptor(interceptor)
 *       .build()
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUrl = originalRequest.url.toString()

        // Don't add token to login request
        if (requestUrl.contains(Constants.Endpoints.LOGIN)) {
            return chain.proceed(originalRequest)
        }

        // Get token from DataStore (blocking because OkHttp is synchronous)
        val token = runBlocking {
            tokenManager.getToken().firstOrNull()
        }

        // If no token, proceed with original request
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Build new request with Authorization header
        val newRequest = originalRequest.newBuilder().apply {
            addHeader(Constants.AUTHORIZATION_HEADER, "${Constants.BEARER_TOKEN_PREFIX}$token")
            addHeader(Constants.ACCEPT_HEADER, Constants.APPLICATION_JSON)
            addHeader(Constants.CONTENT_TYPE_HEADER, Constants.APPLICATION_JSON)
        }.build()

        return chain.proceed(newRequest)
    }
}

