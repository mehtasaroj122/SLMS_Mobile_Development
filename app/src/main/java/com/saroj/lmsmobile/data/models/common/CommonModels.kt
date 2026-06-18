package com.saroj.lmsmobile.data.models.common

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
    val data: List<T>,
    val links: PaginationLinks?,
    val meta: PaginationMeta?
)

data class PaginationLinks(
    val first: String?,
    val last: String?,
    val prev: String?,
    val next: String?
)

data class PaginationMeta(
    val current_page: Int,
    val from: Int?,
    val last_page: Int,
    val path: String?,
    val per_page: Int,
    val to: Int?,
    val total: Int
)

