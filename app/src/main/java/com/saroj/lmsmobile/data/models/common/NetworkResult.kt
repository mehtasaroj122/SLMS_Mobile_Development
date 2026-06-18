package com.saroj.lmsmobile.data.models.common

/**
 * Sealed class to handle network responses uniformly across the app.
 * Provides a clean way to represent Success, Error, Loading, and Unauthorized states.
 *
 * Usage in Repository:
 *   return try {
 *       val response = apiService.getBooks()
 *       NetworkResult.Success(response)
 *   } catch (e: Exception) {
 *       NetworkResult.Error("Failed to fetch books", null)
 *   }
 *
 * Usage in ViewModel:
 *   repository.getBooks().collect { result ->
 *       when (result) {
 *           is NetworkResult.Success -> { ... }
 *           is NetworkResult.Error -> { ... }
 *           is NetworkResult.Loading -> { ... }
 *           is NetworkResult.Unauthorized -> { ... }
 *       }
 *   }
 */
sealed class NetworkResult<T> {

    /**
     * Success state with data of type T.
     * @param data The successful response data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Error state with error message and optional HTTP status code.
     * @param message Human-readable error message
     * @param code HTTP status code (e.g., 404, 422, 500)
     */
    data class Error<T>(
        val message: String,
        val code: Int? = null
    ) : NetworkResult<T>()

    /**
     * Loading state - represents an ongoing network request.
     */
    class Loading<T> : NetworkResult<T>()

    /**
     * Unauthorized state - represents 401 responses (expired token, not authenticated).
     * Use this to trigger logout and redirect to login screen.
     */
    class Unauthorized<T> : NetworkResult<T>()
}

/**
 * Extension function to safely map success data.
 * Usage: result.mapSuccess { it.data }
 */
inline fun <T, R> NetworkResult<T>.mapSuccess(block: (T) -> R): NetworkResult<R> {
    return when (this) {
        is NetworkResult.Success -> NetworkResult.Success(block(this.data))
        is NetworkResult.Error -> NetworkResult.Error(this.message, this.code)
        is NetworkResult.Loading -> NetworkResult.Loading()
        is NetworkResult.Unauthorized -> NetworkResult.Unauthorized()
    }
}

/**
 * Extension function to handle all cases with a block.
 * Usage: result.handle(
 *     onSuccess = { ... },
 *     onError = { message, code -> ... }
 * )
 */
inline fun <T> NetworkResult<T>.handle(
    onSuccess: (T) -> Unit,
    onError: (String, Int?) -> Unit,
    onLoading: () -> Unit = {},
    onUnauthorized: () -> Unit = {}
) {
    when (this) {
        is NetworkResult.Success -> onSuccess(this.data)
        is NetworkResult.Error -> onError(this.message, this.code)
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Unauthorized -> onUnauthorized()
    }
}

