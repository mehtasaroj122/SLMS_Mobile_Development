package com.saroj.lmsmobile.data.models.bookrequest

import com.google.gson.annotations.SerializedName

data class BookRequestSummaryResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: BookRequestSummaryData? = null
)

data class BookRequestSummaryData(
    val total: Int? = null,
    val pending: Int? = null,
    val approved: Int? = null,
    val rejected: Int? = null
)

data class StaffBookRequestListResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: List<StaffBookRequestItem>? = null
)

data class StaffBookRequestItem(
    val id: Int? = null,
    @SerializedName("request_id")
    val requestId: Int? = null,
    val student: RequestStudentInfo? = null,
    val book: RequestBookInfo? = null,
    @SerializedName("request_date")
    val requestDate: String? = null,
    val status: String? = null,
    @SerializedName("processed_by")
    val processedBy: String? = null,
    @SerializedName("processed_at")
    val processedAt: String? = null,
    @SerializedName("processed_date")
    val processedDate: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class RequestStudentInfo(
    val id: Int? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    val name: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("roll_no")
    val rollNo: String? = null,
    @SerializedName("symbol_no")
    val symbolNo: String? = null,
    val department: String? = null,
    val photo: String? = null,
    @SerializedName("photo_url")
    val photoUrl: String? = null
)

data class RequestBookInfo(
    val id: Int? = null,
    val title: String? = null,
    val author: String? = null
)

data class BookRequestActionResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: BookRequestActionData? = null
)

data class BookRequestActionData(
    val id: Int? = null,
    @SerializedName("request_id")
    val requestId: Int? = null,
    val status: String? = null,
    @SerializedName("processed_by")
    val processedBy: String? = null,
    @SerializedName("processed_at")
    val processedAt: String? = null,
    @SerializedName("processed_date")
    val processedDate: String? = null
)
