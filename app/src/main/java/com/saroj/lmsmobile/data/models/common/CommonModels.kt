package com.saroj.lmsmobile.data.models.common

import com.google.gson.annotations.SerializedName

/**
 * ErrorResponse represents API error responses.
 * Used for handling error messages from Laravel API.
 */
data class ErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
    val status: Int? = null
)

/**
 * PaginatedResponse wraps paginated API responses.
 * Generic type T allows this to work with any data model.
 *
 * Usage:
 *   val response = Response<PaginatedResponse<Book>>
 *   if (response.isSuccessful) {
 *       val books = response.body()?.data
 *       val total = response.body()?.total
 *   }
 */
data class PaginatedResponse<T>(
    val data: List<T> = emptyList(),
    val current_page: Int = 1,
    val per_page: Int = 20,
    val total: Int = 0,
    val last_page: Int = 1,
    @SerializedName("from")
    val from: Int? = null,
    @SerializedName("to")
    val to: Int? = null
) {
    val hasNextPage: Boolean
        get() = current_page < last_page

    val hasPreviousPage: Boolean
        get() = current_page > 1
}

