package com.saroj.lmsmobile.data.models.staffdashboard

import com.google.gson.annotations.SerializedName

data class StaffDashboardEnvelope(
    val message: String? = null,
    val data: StaffDashboardData? = null
)

data class StaffDashboardData(
    val staff: StaffInfo? = null,
    @SerializedName("profile_photo")
    val profilePhoto: String? = null,
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    @SerializedName("total_books")
    val totalBooks: Int? = null,
    @SerializedName("total_students")
    val totalStudents: Int? = null,
    @SerializedName("issued_books")
    val issuedBooks: Int? = null,
    @SerializedName("returned_books")
    val returnedBooks: Int? = null,
    @SerializedName("overdue_books")
    val overdueBooksCount: Int? = null,
    @SerializedName("pending_fines")
    val pendingFines: Double? = null,
    @SerializedName("pending_requests")
    val pendingRequestsCount: Int? = null,
    val summary: StaffDashboardSummary? = null,
    val circulation: BookCirculationSummary? = null,
    @SerializedName("weekly_activity")
    val weeklyActivity: WeeklyActivitySummary? = null,
    @SerializedName("pending_requests_list")
    val pendingRequests: List<PendingRequestItem>? = null,
    @SerializedName("due_today")
    val dueToday: List<DueTodayItem>? = null,
    @SerializedName("overdue_books_list")
    val overdueBooks: List<OverdueBookItem>? = null,
    @SerializedName("recent_issues")
    val recentIssues: List<RecentIssueItem>? = null
)

data class StaffInfo(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerializedName("staff_id")
    val staffId: String? = null,
    val department: String? = null,
    val designation: String? = null
)

data class StaffDashboardSummary(
    @SerializedName("currently_issued")
    val currentlyIssued: Int? = null,
    @SerializedName("due_today")
    val dueToday: Int? = null,
    val overdue: Int? = null,
    @SerializedName("pending_requests")
    val pendingRequests: Int? = null,
    @SerializedName("pending_fines_amount")
    val pendingFinesAmount: Double? = null
)

data class BookCirculationSummary(
    @SerializedName("available_books")
    val availableBooks: Int? = null,
    @SerializedName("issued_on_time")
    val issuedOnTime: Int? = null,
    @SerializedName("due_today")
    val dueToday: Int? = null,
    val overdue: Int? = null,
    @SerializedName("total_trackable_copies")
    val totalTrackableCopies: Int? = null
)

data class WeeklyActivitySummary(
    @SerializedName("issued_this_week")
    val issuedThisWeek: Int? = null,
    @SerializedName("returned_this_week")
    val returnedThisWeek: Int? = null,
    @SerializedName("peak_activity_day")
    val peakActivityDay: String? = null,
    @SerializedName("average_per_day")
    val averagePerDay: Double? = null
)

data class PendingRequestItem(
    val id: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    @SerializedName("student_name")
    val studentName: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("symbol_no")
    val symbolNo: String? = null,
    @SerializedName("request_date")
    val requestDate: String? = null,
    val status: String? = null
)

data class DueTodayItem(
    val id: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    @SerializedName("student_name")
    val studentName: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("issue_id")
    val issueId: Int? = null
)

data class OverdueBookItem(
    val id: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    @SerializedName("student_name")
    val studentName: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("days_overdue")
    val daysOverdue: Int? = null,
    @SerializedName("issue_id")
    val issueId: Int? = null
)

data class RecentIssueItem(
    val id: Int? = null,
    @SerializedName("book_title")
    val bookTitle: String? = null,
    @SerializedName("student_name")
    val studentName: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    @SerializedName("issued_date")
    val issuedDate: String? = null,
    @SerializedName("time_ago")
    val timeAgo: String? = null,
    @SerializedName("issue_id")
    val issueId: Int? = null
)

data class RejectBookRequestBody(
    val remarks: String? = null
)
