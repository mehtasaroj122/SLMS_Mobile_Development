package com.saroj.lmsmobile.data.repository

import com.saroj.lmsmobile.api.ApiService
import com.google.gson.Gson
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.book.StudentBookRequestBody
import com.saroj.lmsmobile.data.models.book.StudentBookRequestResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.common.PaginatedResponse
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

/**
 * BookRepository handles all book-related API calls.
 */
class BookRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Get all books with pagination.
     */
    fun getBooks(page: Int): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getBooks(page)
        handlePaginatedResponse(response).collect { emit(it) }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    /**
     * Get book details by ID.
     */
    fun getBookDetail(id: Int): Flow<NetworkResult<Book>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getBookDetail(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error("Book not found", response.code()))
                }
            } else {
                handleError(response).collect { emit(it) }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    /**
     * Search books by query.
     */
    fun searchBooks(query: String, page: Int): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.searchBooks(query, page)
            handlePaginatedResponse(response).collect { emit(it) }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    /**
     * Get available books.
     */
    fun getAvailableBooks(page: Int = 1, pageSize: Int = 20): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getAvailableBooks(page, pageSize)
            handlePaginatedResponse(response).collect { emit(it) }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    /**
     * Submit a student request for a book.
     */
    fun submitStudentBookRequest(bookId: Int): Flow<NetworkResult<StudentBookRequestResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.submitStudentBookRequest(StudentBookRequestBody(bookId))
        if (response.isSuccessful) {
            emit(
                NetworkResult.Success(
                    response.body() ?: StudentBookRequestResponse("Request submitted successfully.", null)
                )
            )
        } else {
            handleError(response).collect { emit(it) }
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    /**
     * Helper to handle paginated responses.
     */
    private fun <T> handlePaginatedResponse(response: Response<PaginatedResponse<T>>): Flow<NetworkResult<PaginatedResponse<T>>> = flow {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                emit(NetworkResult.Success(body))
            } else {
                emit(NetworkResult.Error("Empty response", response.code()))
            }
        } else {
            when (response.code()) {
                Constants.HTTP_UNAUTHORIZED -> {
                    tokenManager.clearAllData()
                    emit(NetworkResult.Unauthorized())
                }
                Constants.HTTP_FORBIDDEN -> emit(NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code()))
                Constants.HTTP_NOT_FOUND -> emit(NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code()))
                else -> emit(NetworkResult.Error(response.message() ?: Constants.ERROR_UNKNOWN, response.code()))
            }
        }
    }

    /**
     * Helper to handle API errors.
     */
    private fun <T> handleError(response: Response<T>): Flow<NetworkResult<T>> = flow {
        val message = parseErrorMessage(response)
        when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                emit(NetworkResult.Unauthorized())
            }
            Constants.HTTP_FORBIDDEN -> emit(NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code()))
            Constants.HTTP_NOT_FOUND -> emit(NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code()))
            Constants.HTTP_UNPROCESSABLE_ENTITY -> emit(NetworkResult.Error(message, response.code()))
            else -> emit(NetworkResult.Error(message, response.code()))
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) {
            return response.message().takeIf { it.isNotBlank() } ?: Constants.ERROR_UNKNOWN
        }

        val parsed = runCatching {
            Gson().fromJson(rawError, ErrorResponse::class.java)
        }.getOrNull()

        val firstValidationError = parsed?.errors
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()

        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(140)
    }
}
