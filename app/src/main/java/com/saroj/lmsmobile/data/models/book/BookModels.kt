package com.saroj.lmsmobile.data.models.book

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Book represents a book in the library system.
 */
data class Book(
    val id: Int,
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val edition: String? = null,
    val category: String? = null,
    @SerializedName("total_copies")
    val totalCopies: Int? = 0,
    @SerializedName("available_copies")
    val availableCopies: Int? = 0,
    val description: String? = null,
    @SerializedName("publication_year")
    val publicationYear: Int? = null,
    val cover_image: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable

/**
 * BookResponse wraps book data from API.
 */
data class BookResponse(
    val message: String? = null,
    val data: Book
)

/**
 * BookRequestModel represents a book request (student request for a book).
 */
data class BookRequestModel(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("book_id")
    val bookId: Int,
    val status: String, // pending, approved, rejected
    val reason: String? = null,
    @SerializedName("requested_at")
    val requestedAt: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

