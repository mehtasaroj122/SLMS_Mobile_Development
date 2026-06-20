package com.saroj.lmsmobile.data.models.studentdashboard

data class StudentDashboardEnvelope(
    val message: String? = null,
    val data: StudentDashboardResponse? = null
)

data class StudentDashboardResponse(
    val message: String?,
    val data: StudentDashboardData?,
    val student: DashboardStudent?,
    val stats: DashboardStats?,
    val currently_issued: List<DashboardIssuedBook>?,
    val due_soon: List<DashboardIssuedBook>?,
    val latest_notifications: List<DashboardNotification>?,
    val privileges: DashboardPrivileges?
)

data class StudentDashboardData(
    val student: DashboardStudent?,
    val stats: DashboardStats?,
    val currently_issued: List<DashboardIssuedBook>?,
    val due_soon: List<DashboardIssuedBook>?,
    val latest_notifications: List<DashboardNotification>?,
    val privileges: DashboardPrivileges?
)

data class DashboardStudent(
    val id: Int?,
    val name: String?,
    val roll_no: String?,
    val email: String?,
    val phone: String?,
    val faculty: String?,
    val semester: String?,
    val year_of_study: Int?,
    val status: String?,
    val profile_photo: String?,
    val profile_photo_url: String?
)

data class DashboardStats(
    val issued_books: Int?,
    val returned_books: Int?,
    val pending_fines: Double?,
    val active_requests: Int?,
    val total_requests: Int?,
    val approved_requests: Int?,
    val rejected_requests: Int?
)

data class DashboardIssuedBook(
    val issue_id: Int?,
    val book: DashboardBook?,
    val issue_date: String?,
    val due_date: String?,
    val return_date: String?,
    val status: String?
)

data class DashboardBook(
    val id: Int?,
    val title: String?,
    val author: String?,
    val accession_no: String?
)

data class DashboardNotification(
    val id: Int?,
    val title: String?,
    val message: String?,
    val type: String?,
    val created_at: String?,
    val read_at: String?
)

data class DashboardPrivileges(
    val max_books: Int?,
    val borrow_days: Int?,
    val fine_per_day: Double?,
    val borrowing_status: String?,
    val current_usage: Int?,
    val remaining_books: Int?,
    val is_custom: Boolean?,
    val setting_type: String?,
    val source: String?,
    val type: String?
)
