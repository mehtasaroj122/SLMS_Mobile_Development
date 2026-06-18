package com.saroj.lmsmobile.data.models.issue

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Issue represents a book borrowing record.
 * Tracks when a student borrowed a book and when it's due to be returned.
 */
data class Issue(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("book_id")
    val bookId: Int,
    val student: Student? = null,
    val book: Book? = null,
    @SerializedName("issue_date")
    val issueDate: String,
    @SerializedName("due_date")
    val dueDate: String,
    @SerializedName("return_date")
    val returnDate: String? = null,
    val status: String, // issued, returned, overdue
    val remarks: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable {
    val isOverdue: Boolean
        get() = status == "overdue" && returnDate == null

    val isReturned: Boolean
        get() = status == "returned"
}

/**
 * IssueRequest represents the request body for creating a new book issue.
 */
data class IssueRequest(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("book_id")
    val bookId: Int,
    val remarks: String? = null
)

/**
 * IssueResponse wraps issue data from API.
 */
data class IssueResponse(
    val message: String? = null,
    val data: Issue
)

/**
 * Nested Student in Issue response.
 */
data class Student(
    val id: Int,
    val name: String,
    @SerializedName("student_id")
    val studentId: String? = null,
    val email: String? = null
) : Serializable

/**
 * Nested Book in Issue response.
 */
data class Book(
    val id: Int,
    val title: String,
    val author: String? = null,
    val isbn: String? = null
) : Serializable

