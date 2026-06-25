package com.saroj.lmsmobile.api

import com.saroj.lmsmobile.data.models.auth.LoginRequest
import com.saroj.lmsmobile.data.models.auth.LoginResponse
import com.saroj.lmsmobile.data.models.auth.ProfileResponse
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.book.BookRequestModel
import com.saroj.lmsmobile.data.models.book.StudentBookRequestBody
import com.saroj.lmsmobile.data.models.book.StudentBookRequestResponse
import com.saroj.lmsmobile.data.models.common.PaginatedResponse
import com.saroj.lmsmobile.data.models.dashboard.DashboardResponse
import com.saroj.lmsmobile.data.models.fine.Fine
import com.saroj.lmsmobile.data.models.fine.FineActionResponse
import com.saroj.lmsmobile.data.models.fine.FineStudentListResponse
import com.saroj.lmsmobile.data.models.fine.FineSummaryResponse
import com.saroj.lmsmobile.data.models.fine.StudentFineDetailResponse
import com.saroj.lmsmobile.data.models.fine.WaiveFineRequest
import com.saroj.lmsmobile.data.models.issue.Issue
import com.saroj.lmsmobile.data.models.issue.IssueBooksRequest
import com.saroj.lmsmobile.data.models.issue.IssueRequest
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnRequest
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewRequest
import com.saroj.lmsmobile.data.models.returnbook.SingleReturnRequest
import com.saroj.lmsmobile.data.models.profile.ChangePasswordRequest
import com.saroj.lmsmobile.data.models.profile.DeleteAccountRequest
import com.saroj.lmsmobile.data.models.profile.ProfileUpdateRequest
import com.saroj.lmsmobile.data.models.staffdashboard.RejectBookRequestBody
import com.saroj.lmsmobile.data.models.staffdashboard.StaffDashboardEnvelope
import com.saroj.lmsmobile.data.models.student.Student
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardEnvelope
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ApiService defines all REST API endpoints for the LMS Mobile App.
 * This interface is implemented by Retrofit at runtime.
 *
 * Features:
 * - Authentication endpoints
 * - Book management endpoints
 * - Student management endpoints
 * - Issue/Borrowing endpoints
 * - Fine management endpoints
 * - Dashboard endpoints
 *
 * All protected endpoints automatically receive Bearer token via AuthInterceptor.
 *
 * Usage:
 *   val apiService = RetrofitClient.getApiService(tokenManager)
 *   try {
 *       val response = apiService.login(LoginRequest(...))
 *       // Handle response
 *   } catch (e: Exception) {
 *       // Handle error
 *   }
 */
interface ApiService {

    // ==================== AUTHENTICATION ====================

    /**
     * Login with email and password.
     * Returns access token and user information.
     */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Logout and invalidate current token.
     */
    @POST("logout")
    suspend fun logout(): Response<Any>

    /**
     * Get current logged-in user profile.
     */
    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @GET("profile")
    suspend fun getProfileJson(): Response<JsonElement>

    @PUT("profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<JsonElement>

    @POST("profile/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<JsonElement>

    @POST("profile/password")
    suspend fun changeProfilePassword(@Body request: ChangePasswordRequest): Response<JsonElement>

    @GET("profile/delete-eligibility")
    suspend fun getProfileDeleteEligibility(): Response<JsonElement>

    @Multipart
    @POST("profile/photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<JsonElement>

    @DELETE("profile/photo")
    suspend fun removeProfilePhoto(): Response<JsonElement>

    @DELETE("profile")
    suspend fun deleteProfile(): Response<JsonElement>

    @HTTP(method = "DELETE", path = "profile/account", hasBody = true)
    suspend fun deleteProfileAccount(@Body request: DeleteAccountRequest): Response<JsonElement>

    /**
     * Test endpoint to verify if token is valid (returns 401 if invalid).
     */
    @GET("test-unauthorized")
    suspend fun testUnauthorized(): Response<Any>

    // ==================== BOOKS ====================

    /**
     * Get all available books with pagination.
     * @param page Page number (default: 1)
     */
    @GET("books")
    suspend fun getBooks(
        @Query("page") page: Int,
        @Query("category") category: String? = null,
        @Query("availability") availability: String? = null,
        @Query("condition") condition: String? = null,
        @Query("sort") sort: String? = null,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Book>>

    /**
     * Get book details by ID.
     * @param id Book ID
     */
    @GET("books/{id}")
    suspend fun getBookDetail(@Path("id") id: Int): Response<Book>

    @GET("books/{id}")
    suspend fun getBookDetailJson(@Path("id") id: Int): Response<JsonElement>

    /**
     * Search books by query string.
     * @param query Search keyword (title, author, ISBN)
     */
    @GET("books/search")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("category") category: String? = null,
        @Query("availability") availability: String? = null,
        @Query("condition") condition: String? = null,
        @Query("sort") sort: String? = null,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Book>>

    /**
     * Submit an authenticated student request for a book.
     */
    @POST("student/requests")
    suspend fun submitStudentBookRequest(
        @Body request: StudentBookRequestBody
    ): Response<StudentBookRequestResponse>

    /**
     * Get authenticated student's book requests.
     */
    @GET("student/requests")
    suspend fun getStudentBookRequests(
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<BookRequestModel>>

    @GET("student/requests/summary")
    suspend fun getStudentRequestsSummary(): Response<JsonElement>

    @GET("student/requests")
    suspend fun getAuthenticatedStudentRequests(): Response<JsonElement>

    @GET("student/requests/{id}")
    suspend fun getAuthenticatedStudentRequestDetail(@Path("id") id: Int): Response<JsonElement>

    @POST("student/requests/{id}/cancel")
    suspend fun cancelAuthenticatedStudentRequest(@Path("id") id: Int): Response<JsonElement>

    /**
     * Get all available books (not currently issued).
     */
    @GET("books/available")
    suspend fun getAvailableBooks(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Book>>

    /**
     * Get books by category.
     * @param category Category name or ID
     */
    @GET("books/category/{category}")
    suspend fun getBooksByCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Book>>

    // ==================== STUDENTS ====================

    /**
     * Get all students (Admin & Staff only).
     */
    @GET("students")
    suspend fun getStudents(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Student>>

    /**
     * Get student details by ID.
     * @param id Student ID
     */
    @GET("students/{id}")
    suspend fun getStudentDetail(@Path("id") id: Int): Response<Student>

    @GET("students/{id}")
    suspend fun getStudentDetailJson(@Path("id") id: Int): Response<JsonElement>

    /**
     * Search students by query string.
     * @param query Search keyword (name, email, student ID)
     */
    @GET("students/search")
    suspend fun searchStudents(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Student>>

    @GET("students/search")
    suspend fun searchStudentsJson(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<JsonElement>

    // ==================== ISSUES (Book Borrowing) ====================

    /**
     * Get all book issues/borrowing records.
     */
    @GET("issues")
    suspend fun getIssues(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Issue>>

    /**
     * Get issue details by ID.
     * @param id Issue ID
     */
    @GET("issues/{id}")
    suspend fun getIssueDetail(@Path("id") id: Int): Response<Issue>

    /**
     * Create a new book issue (issue a book to a student).
     */
    @POST("issues")
    suspend fun createIssue(@Body request: IssueRequest): Response<Issue>

    /**
     * Mark book as returned.
     * @param id Issue ID
     */
    @POST("issues/return/{id}")
    suspend fun returnBook(@Path("id") id: Int): Response<Issue>

    /**
     * Get all issues for a specific student.
     * @param studentId Student ID
     */
    @GET("issues/student/{studentId}")
    suspend fun getStudentIssues(
        @Path("studentId") studentId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Issue>>

    // ==================== DASHBOARD ====================

    /**
     * Get dashboard statistics and summary data.
     * Content varies based on user role (Admin/Staff/Student).
     */
    @GET("dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    /**
     * Get the authenticated student's dashboard summary.
     */
    @GET("student/dashboard")
    suspend fun getStudentDashboard(): Response<StudentDashboardEnvelope>

    @GET("staff/dashboard")
    suspend fun getStaffDashboard(): Response<StaffDashboardEnvelope>

    @GET("staff/students/search")
    suspend fun searchStaffIssueStudents(
        @Query("query") query: String,
        @Query("q") q: String = query,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<JsonElement>

    @GET("staff/students/{studentId}")
    suspend fun getStaffIssueStudentDetail(
        @Path("studentId") studentId: Int
    ): Response<JsonElement>

    @GET("staff/students/{studentId}/issue-privileges")
    suspend fun getStaffStudentIssuePrivileges(
        @Path("studentId") studentId: Int
    ): Response<JsonElement>

    @GET("staff/books/search")
    suspend fun searchStaffIssueBooks(
        @Query("query") query: String,
        @Query("q") q: String = query,
        @Query("student_id") studentId: Int,
        @Query("studentId") studentIdCamel: Int = studentId,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<JsonElement>

    @POST("staff/issues")
    suspend fun issueStaffBooks(
        @Body request: IssueBooksRequest
    ): Response<JsonElement>

    @GET("staff/returns/settings")
    suspend fun getReturnSettings(): Response<JsonElement>

    @GET("staff/returns/students/search")
    suspend fun searchReturnStudents(
        @Query("query") query: String,
        @Query("q") q: String = query
    ): Response<JsonElement>

    @GET("staff/returns/students/{studentId}")
    suspend fun getStudentReturnData(
        @Path("studentId") studentId: Int
    ): Response<JsonElement>

    @POST("staff/returns/preview")
    suspend fun previewReturnFine(
        @Body request: ReturnPreviewRequest
    ): Response<JsonElement>

    @POST("staff/returns")
    suspend fun processReturns(
        @Body request: ProcessReturnRequest
    ): Response<JsonElement>

    @GET("staff/issues/search")
    suspend fun searchStaffActiveIssues(
        @Query("query") query: String,
        @Query("q") q: String = query
    ): Response<JsonElement>

    @POST("staff/issues/{issueId}/return")
    suspend fun returnStaffIssue(
        @Path("issueId") issueId: Int,
        @Body request: SingleReturnRequest
    ): Response<JsonElement>

    @GET("staff/book-requests/summary")
    suspend fun getStaffBookRequestSummary(): Response<JsonElement>

    @GET("staff/book-requests")
    suspend fun getStaffBookRequests(
        @Query("status") status: String,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<JsonElement>

    @POST("staff/book-requests/{requestId}/approve")
    suspend fun approveStaffBookRequest(@Path("requestId") requestId: Int): Response<JsonElement>

    @POST("staff/book-requests/{requestId}/reject")
    suspend fun rejectStaffBookRequest(
        @Path("requestId") requestId: Int,
        @Body request: RejectBookRequestBody = RejectBookRequestBody()
    ): Response<JsonElement>

    @GET("library/settings")
    suspend fun getLibrarySettings(): Response<JsonElement>

    @POST("book-requests/{id}/approve")
    suspend fun approveBookRequest(@Path("id") id: Int): Response<JsonElement>

    @POST("book-requests/{id}/reject")
    suspend fun rejectBookRequest(
        @Path("id") id: Int,
        @Body request: RejectBookRequestBody = RejectBookRequestBody()
    ): Response<JsonElement>

    @GET("student/my-books/summary")
    suspend fun getStudentMyBooksSummary(): Response<JsonElement>

    @GET("student/my-books/current")
    suspend fun getStudentMyBooksCurrent(): Response<JsonElement>

    @GET("student/my-books/history")
    suspend fun getStudentMyBooksHistory(): Response<JsonElement>

    @GET("student/my-books/due-soon")
    suspend fun getStudentMyBooksDueSoon(): Response<JsonElement>

    @GET("student/fines/summary")
    suspend fun getStudentFinesSummary(): Response<JsonElement>

    @GET("student/fines")
    suspend fun getAuthenticatedStudentFines(): Response<JsonElement>

    @GET("student/fines/pending")
    suspend fun getAuthenticatedStudentPendingFines(): Response<JsonElement>

    @GET("student/fines/paid")
    suspend fun getAuthenticatedStudentPaidFines(): Response<JsonElement>

    @GET("student/fines/{id}")
    suspend fun getAuthenticatedStudentFineDetail(@Path("id") id: Int): Response<JsonElement>

    @GET("staff/fines/summary")
    suspend fun getStaffFineSummary(): Response<FineSummaryResponse>

    @GET("staff/fines/students")
    suspend fun getStaffFineStudents(
        @Query("search") search: String? = null
    ): Response<FineStudentListResponse>

    @GET("staff/fines/students/{student}")
    suspend fun getStudentFineDetails(
        @Path("student") studentId: Int
    ): Response<StudentFineDetailResponse>

    @POST("staff/fines/{fine}/pay")
    suspend fun markFinePaid(
        @Path("fine") fineId: Int
    ): Response<FineActionResponse>

    @POST("staff/fines/{fine}/waive")
    suspend fun waiveFine(
        @Path("fine") fineId: Int,
        @Body request: WaiveFineRequest
    ): Response<FineActionResponse>

    // ==================== NOTIFICATIONS ====================

    @GET("notifications")
    suspend fun getNotifications(): Response<JsonElement>

    @GET("notifications/unread")
    suspend fun getUnreadNotifications(): Response<JsonElement>

    @GET("notifications/count")
    suspend fun getNotificationCount(): Response<JsonElement>

    @POST("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<JsonElement>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<JsonElement>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<JsonElement>

    // ==================== OVERDUE ====================

    /**
     * Get all overdue book issues.
     */
    @GET("overdue")
    suspend fun getOverdueIssues(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Issue>>

    // ==================== FINES ====================

    /**
     * Get all fines.
     */
    @GET("fines")
    suspend fun getFines(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Fine>>

    /**
     * Get all fines for a specific student.
     * @param studentId Student ID
     */
    @GET("fines/student/{studentId}")
    suspend fun getStudentFines(
        @Path("studentId") studentId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Fine>>
}

