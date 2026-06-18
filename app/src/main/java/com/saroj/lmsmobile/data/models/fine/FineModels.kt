package com.saroj.lmsmobile.data.models.fine

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Fine represents a fine imposed on a student for overdue books.
 */
data class Fine(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("issue_id")
    val issueId: Int? = null,
    val amount: Double,
    @SerializedName("fine_date")
    val fineDate: String,
    @SerializedName("paid_date")
    val paidDate: String? = null,
    val status: String, // pending, paid, waived
    val remarks: String? = null,
    val student: FineStudent? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable {
    val isPaid: Boolean
        get() = status == "paid"

    val isPending: Boolean
        get() = status == "pending"
}

/**
 * FineResponse wraps fine data from API.
 */
data class FineResponse(
    val message: String? = null,
    val data: Fine
)

/**
 * Nested Student in Fine response.
 */
data class FineStudent(
    val id: Int,
    val name: String,
    @SerializedName("student_id")
    val studentId: String? = null,
    val email: String? = null
) : Serializable

