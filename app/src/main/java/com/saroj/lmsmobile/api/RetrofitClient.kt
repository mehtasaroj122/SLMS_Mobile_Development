package com.saroj.lmsmobile.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.saroj.lmsmobile.api.interceptor.AuthInterceptor
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient provides a singleton instance of Retrofit for API communication.
 *
 * Features:
 * - Singleton pattern (lazy initialization)
 * - OkHttpClient with custom configuration
 * - HttpLoggingInterceptor for debugging (debug builds only)
 * - AuthInterceptor for automatic token attachment
 * - Timeout configuration
 * - Gson converter with proper settings
 *
 * Usage:
 *   val apiService = RetrofitClient.getApiService(context)
 *   val books = apiService.getBooks()
 *
 * Architecture:
 *   Request → AuthInterceptor (adds token) → HttpLogging (logs request)
 *   → OkHttpClient → Server
 *   Response → HttpLogging (logs response) → Retrofit → ViewModel
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"
    private const val MAX_ERROR_LOG_BYTES = 8_192L

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    /**
     * Gets or creates the Retrofit instance.
     * @param tokenManager TokenManager for authentication
     * @return Retrofit instance configured with OkHttpClient
     */
    private fun getRetrofit(tokenManager: TokenManager): Retrofit {
        return retrofit ?: createRetrofit(tokenManager).also { retrofit = it }
    }

    /**
     * Gets or creates the ApiService instance.
     * @param tokenManager TokenManager for authentication
     * @return ApiService interface implementation
     */
    fun getApiService(tokenManager: TokenManager): ApiService {
        return apiService ?: getRetrofit(tokenManager).create(ApiService::class.java).also {
            apiService = it
        }
    }

    /**
     * Creates a new Retrofit instance with full configuration.
     */
    private fun createRetrofit(tokenManager: TokenManager): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(createOkHttpClient(tokenManager))
            .addConverterFactory(createGsonConverterFactory())
            .build()
    }

    /**
     * Creates OkHttpClient with logging and authentication interceptors.
     */
    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val builder = OkHttpClient.Builder()

        // Add Auth Interceptor (adds Bearer token to all requests)
        builder.addInterceptor(AuthInterceptor(tokenManager))
        builder.addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (!response.isSuccessful) {
                val errorBody = runCatching {
                    response.peekBody(MAX_ERROR_LOG_BYTES).string()
                }.getOrDefault("")
                Log.e(
                    TAG,
                    "API request failed. url=${request.url}, status=${response.code}, errorBody=${errorBody.ifBlank { "<empty>" }}"
                )
            }
            response
        }

        // Add HttpLogging Interceptor (only in debug builds)
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader(Constants.AUTHORIZATION_HEADER)
            level = HttpLoggingInterceptor.Level.BODY
        }
        builder.addInterceptor(httpLoggingInterceptor)

        // Set timeouts
        builder.connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        builder.readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
        builder.writeTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)

        return builder.build()
    }

    /**
     * Creates Gson converter factory with proper settings.
     */
    private fun createGsonConverterFactory(): GsonConverterFactory {
        val gson = GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create()

        return GsonConverterFactory.create(gson)
    }

    /**
     * Resets the Retrofit instance (useful for switching base URL or token).
     * Call this if you need to reinitialize Retrofit with new configuration.
     */
    fun reset() {
        retrofit = null
        apiService = null
    }
}

