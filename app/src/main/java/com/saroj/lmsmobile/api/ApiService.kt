package com.saroj.lmsmobile.api

import com.saroj.lmsmobile.data.models.auth.LoginRequest
import com.saroj.lmsmobile.data.models.auth.LoginResponse
import com.saroj.lmsmobile.data.models.auth.ProfileResponse
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.PaginatedResponse
import com.saroj.lmsmobile.data.models.dashboard.DashboardResponse
import com.saroj.lmsmobile.data.models.fine.Fine
import com.saroj.lmsmobile.data.models.issue.Issue
import com.saroj.lmsmobile.data.models.issue.IssueRequest
import com.saroj.lmsmobile.data.models.student.Student
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

    /**
     * Test endpoint to verify if token is valid (returns 401 if invalid).
     */
    @GET("test-unauthorized")
    suspend fun testUnauthorized(): Response<Any>

    // ==================== BOOKS ====================

    /**
     * Get all available books with pagination.
     * @param page Page number (default: 1)
     * @param pageSize Items per page (default: 20)
     */
    @GET("books")
    suspend fun getBooks(
        @Query("page") page: Int = 1,
        @Query("per_page") pageSize: Int = 20
    ): Response<PaginatedResponse<Book>>

    /**
     * Get book details by ID.
     * @param id Book ID
     */
    @GET("books/{id}")
    suspend fun getBookDetail(@Path("id") id: Int): Response<Book>

    /**
     * Search books by query string.
     * @param query Search keyword (title, author, ISBN)
     */
    @GET("books/search")
    suspend fun searchBooks(@Query("q") query: String): Response<PaginatedResponse<Book>>

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

