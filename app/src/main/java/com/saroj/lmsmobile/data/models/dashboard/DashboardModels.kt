package com.saroj.lmsmobile.data.models.dashboard

import com.google.gson.annotations.SerializedName

/**
 * DashboardResponse represents the dashboard statistics data.
 * Content varies based on user role (Admin, Staff, Student).
 */
data class DashboardResponse(
    val message: String? = null,
    @SerializedName("total_books")
    val totalBooks: Int? = 0,
    @SerializedName("available_books")
    val availableBooks: Int? = 0,
    @SerializedName("issued_books")
    val issuedBooks: Int? = 0,
    @SerializedName("total_students")
    val totalStudents: Int? = 0,
    @SerializedName("active_students")
    val activeStudents: Int? = 0,
    @SerializedName("total_issues")
    val totalIssues: Int? = 0,
    @SerializedName("overdue_issues")
    val overdueIssues: Int? = 0,
    @SerializedName("pending_returns")
    val pendingReturns: Int? = 0,
    @SerializedName("total_fines")
    val totalFines: Double? = 0.0,
    @SerializedName("pending_fines")
    val pendingFines: Double? = 0.0,
    @SerializedName("my_issued_books")
    val myIssuedBooks: Int? = 0,
    @SerializedName("my_overdue_books")
    val myOverdueBooks: Int? = 0,
    @SerializedName("my_fines")
    val myFines: Double? = 0.0,
    @SerializedName("recent_books")
    val recentBooks: List<DashboardBook>? = emptyList(),
    @SerializedName("recent_issues")
    val recentIssues: List<DashboardIssue>? = emptyList()
)

/**
 * Simplified Book model for dashboard display.
 */
data class DashboardBook(
    val id: Int,
    val title: String,
    val author: String? = null,
    @SerializedName("available_copies")
    val availableCopies: Int? = 0
)

/**
 * Simplified Issue model for dashboard display.
 */
data class DashboardIssue(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("book_id")
    val bookId: Int,
    val studentName: String? = null,
    val bookTitle: String? = null,
    @SerializedName("issue_date")
    val issueDate: String,
    @SerializedName("due_date")
    val dueDate: String,
    val status: String
)

