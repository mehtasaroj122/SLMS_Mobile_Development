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
    @SerializedName(value = "accession_no", alternate = ["accession_number", "accessionNo"])
    val accessionNo: String? = null,
    val publisher: String? = null,
    val edition: String? = null,
    val category: String? = null,
    val condition: String? = null,
    @SerializedName(value = "location", alternate = ["rack", "rack_no", "rack_number"])
    val location: String? = null,
    @SerializedName(value = "quantity", alternate = ["total_copies"])
    val quantity: Int? = 0,
    @SerializedName(value = "available_quantity", alternate = ["available_copies"])
    val available_quantity: Int? = 0,
    @SerializedName(value = "availability_status", alternate = ["availability", "status"])
    val availabilityStatus: String? = null,
    @SerializedName(value = "request_state", alternate = ["student_request_state", "request_status"])
    val requestState: String? = null,
    val description: String? = null,
    @SerializedName("publication_year")
    val publicationYear: Int? = null,
    @SerializedName(
        value = "cover_image",
        alternate = ["cover_image_url", "cover_url", "image_url", "cover", "thumbnail_url"]
    )
    val cover_image: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable {
    val totalCopies: Int?
        get() = quantity

    val availableCopies: Int?
        get() = available_quantity
}

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
    val id: Int = 0,
    @SerializedName("student_id")
    val studentId: Int? = null,
    @SerializedName("book_id")
    val bookId: Int? = null,
    val book: Book? = null,
    val status: String? = null, // pending, approved, rejected
    val reason: String? = null,
    @SerializedName(value = "requested_at", alternate = ["request_date"])
    val requestedAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class StudentBookRequestBody(
    @SerializedName("book_id")
    val bookId: Int
)

data class StudentBookRequestResponse(
    val message: String? = null,
    val data: BookRequestModel? = null
)

