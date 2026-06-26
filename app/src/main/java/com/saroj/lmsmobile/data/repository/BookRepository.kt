package com.saroj.lmsmobile.data.repository

import com.saroj.lmsmobile.api.ApiService
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.book.BookRequestModel
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
    private val gson = Gson()

    /**
     * Get all books with pagination.
     */
    fun getBooks(
        page: Int,
        category: String? = null,
        availability: String? = null,
        condition: String? = null,
        sort: String? = null,
        pageSize: Int = 20
    ): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getBooksJson(page, category, availability, condition, pageSize)
        handleBookListResponse(response, page, pageSize).collect { emit(it) }
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
    fun searchBooks(
        query: String,
        page: Int,
        category: String? = null,
        availability: String? = null,
        condition: String? = null,
        sort: String? = null,
        pageSize: Int = 20
    ): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.searchBooksJson(query, page, category, availability, condition, pageSize)
            handleBookListResponse(response, page, pageSize).collect { emit(it) }
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
     * Get authenticated student's requests for deriving book button state.
     */
    fun getStudentBookRequests(page: Int = 1): Flow<NetworkResult<PaginatedResponse<BookRequestModel>>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getStudentBookRequests(page)
        handlePaginatedResponse(response).collect { emit(it) }
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

    private fun handleBookListResponse(
        response: Response<JsonElement>,
        fallbackPage: Int,
        pageSize: Int
    ): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                emit(NetworkResult.Success(parseBooksResponse(body, fallbackPage, pageSize)))
            } else {
                emit(NetworkResult.Error("Empty response", response.code()))
            }
        } else {
            emit(handleBookListError(response))
        }
    }

    private suspend fun handleBookListError(response: Response<*>): NetworkResult<PaginatedResponse<Book>> {
        val message = parseErrorMessage(response)
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            Constants.HTTP_NOT_FOUND -> NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code())
            Constants.HTTP_UNPROCESSABLE_ENTITY -> NetworkResult.Error(message, response.code())
            else -> NetworkResult.Error(message, response.code())
        }
    }

    private fun parseBooksResponse(
        root: JsonElement,
        fallbackPage: Int,
        pageSize: Int
    ): PaginatedResponse<Book> {
        val dataArray = findBooksArray(root)
        val books = dataArray.mapNotNull { element ->
            runCatching { gson.fromJson(element, Book::class.java) }.getOrNull()
        }.filter { it.id > 0 && it.title.isNotBlank() }

        val metaObject = findMetaObject(root)
        val currentPage = metaObject?.intValue("current_page", "currentPage", "page") ?: fallbackPage
        val lastPage = metaObject?.intValue("last_page", "lastPage", "pages")
            ?: metaObject?.intValue("last_page_url")?.let { fallbackPage }
            ?: currentPage
        val total = metaObject?.intValue("total", "count") ?: books.size
        val perPage = metaObject?.intValue("per_page", "perPage") ?: pageSize

        return PaginatedResponse(
            data = books,
            links = null,
            meta = com.saroj.lmsmobile.data.models.common.PaginationMeta(
                current_page = currentPage,
                from = metaObject?.intValue("from"),
                last_page = lastPage,
                path = metaObject?.stringValue("path"),
                per_page = perPage,
                to = metaObject?.intValue("to"),
                total = total
            )
        )
    }

    private fun findBooksArray(root: JsonElement): List<JsonElement> {
        if (root.isJsonArray) return root.asJsonArray.toList()
        val rootObject = root.asObjectOrNull() ?: return emptyList()
        val data = rootObject.get("data")

        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            val dataObject = data.asJsonObject
            listOf("data", "books", "items", "results").forEach { key ->
                val nested = dataObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }

        listOf("books", "items", "results").forEach { key ->
            val nested = rootObject.get(key)
            if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
        }
        return emptyList()
    }

    private fun findMetaObject(root: JsonElement): JsonObject? {
        val rootObject = root.asObjectOrNull() ?: return null
        rootObject.objectValue("meta", "pagination")?.let { return it }
        val dataObject = rootObject.get("data").asObjectOrNull()
        return dataObject?.objectValue("meta", "pagination") ?: dataObject ?: rootObject
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

        val parsed = runCatching { gson.fromJson(rawError, ErrorResponse::class.java) }.getOrNull()

        val firstValidationError = parsed?.errors
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()

        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(140)
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? {
        return if (this != null && isJsonObject) asJsonObject else null
    }

    private fun JsonObject.objectValue(vararg keys: String): JsonObject? {
        return keys.firstNotNullOfOrNull { key -> get(key).asObjectOrNull() }
    }

    private fun JsonObject.intValue(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asInt }.getOrNull()
            }
        }
    }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asString }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }
}
