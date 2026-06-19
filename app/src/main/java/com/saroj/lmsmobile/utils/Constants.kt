package com.saroj.lmsmobile.utils

/**
 * Global constants for the SLMS Mobile App.
 * Includes API endpoints, request codes, timeout values, and app settings.
 */
object Constants {

    // ==================== API Configuration ====================
    // Base URL for API requests - Change based on emulator/device

        // for emulator
//    const val BASE_URL = "http://10.0.2.2:8000/api/"

        // for real mobile
    const val BASE_URL = "http://192.168.1.68:8000/api/"

    // API Endpoints
    object Endpoints {
        // Auth
        const val LOGIN = "login"
        const val LOGOUT = "logout"
        const val PROFILE = "profile"
        const val TEST_UNAUTHORIZED = "test-unauthorized"

        // Books
        const val BOOKS = "books"
        const val BOOK_DETAIL = "books/{id}"
        const val SEARCH_BOOKS = "books/search"
        const val AVAILABLE_BOOKS = "books/available"
        const val BOOKS_BY_CATEGORY = "books/category/{category}"

        // Students
        const val STUDENTS = "students"
        const val STUDENT_DETAIL = "students/{id}"
        const val SEARCH_STUDENTS = "students/search"

        // Issues
        const val ISSUES = "issues"
        const val ISSUE_DETAIL = "issues/{id}"
        const val CREATE_ISSUE = "issues"
        const val RETURN_ISSUE = "issues/return/{id}"
        const val STUDENT_ISSUES = "issues/student/{studentId}"

        // Dashboard
        const val DASHBOARD = "dashboard"

        // Overdue
        const val OVERDUE = "overdue"

        // Fines
        const val FINES = "fines"
        const val STUDENT_FINES = "fines/student/{id}"
    }

    // ==================== DataStore Keys ====================
    const val TOKEN_KEY = "access_token"
    const val USER_ID_KEY = "user_id"
    const val USER_NAME_KEY = "user_name"
    const val USER_EMAIL_KEY = "user_email"
    const val USER_ROLE_KEY = "user_role"
    const val IS_LOGGED_IN_KEY = "is_logged_in"

    // ==================== User Roles ====================
    const val ROLE_ADMIN = "admin"
    const val ROLE_STAFF = "staff"
    const val ROLE_STUDENT = "student"

    // ==================== Network Configuration ====================
    const val NETWORK_TIMEOUT = 30L // seconds
    const val CONNECT_TIMEOUT = 15L // seconds
    const val READ_TIMEOUT = 30L // seconds

    // ==================== HTTP Headers ====================
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_TOKEN_PREFIX = "Bearer "
    const val ACCEPT_HEADER = "Accept"
    const val CONTENT_TYPE_HEADER = "Content-Type"
    const val APPLICATION_JSON = "application/json"

    // ==================== HTTP Status Codes ====================
    const val HTTP_OK = 200
    const val HTTP_CREATED = 201
    const val HTTP_UNAUTHORIZED = 401
    const val HTTP_FORBIDDEN = 403
    const val HTTP_NOT_FOUND = 404
    const val HTTP_UNPROCESSABLE_ENTITY = 422
    const val HTTP_SERVER_ERROR = 500

    // ==================== Error Messages ====================
    const val ERROR_NO_INTERNET = "No internet connection. Please check your network."
    const val ERROR_UNAUTHORIZED = "Your session has expired. Please login again."
    const val ERROR_FORBIDDEN = "You don't have permission to access this resource."
    const val ERROR_NOT_FOUND = "Resource not found."
    const val ERROR_VALIDATION = "Please check your input and try again."
    const val ERROR_SERVER = "Server error occurred. Please try again later."
    const val ERROR_UNKNOWN = "An unknown error occurred. Please try again."

    // ==================== UI Configuration ====================
    // Animation durations (milliseconds)
    const val ANIMATION_DURATION_SHORT = 300L
    const val ANIMATION_DURATION_MEDIUM = 500L
    const val ANIMATION_DURATION_LONG = 1000L

    // Fragment and Activity transition durations
    const val FRAGMENT_TRANSITION_DURATION = 300

    // Splash screen duration
    const val SPLASH_SCREEN_DURATION = 2000L

    // ==================== Request Codes ====================
    const val REQUEST_CODE_LOGIN = 1001
    const val REQUEST_CODE_LOGOUT = 1002
    const val REQUEST_CODE_PROFILE = 1003
    const val REQUEST_CODE_BOOKS = 1004
    const val REQUEST_CODE_STUDENTS = 1005

    // ==================== Pagination ====================
    const val DEFAULT_PAGE_SIZE = 20
    const val FIRST_PAGE = 1

    // ==================== Shared Preferences / DataStore ====================
    const val PREFS_NAME = "lms_mobile_app"
    const val PREFS_MODE = android.content.Context.MODE_PRIVATE

    // ==================== Bundle Keys ====================
    const val KEY_BOOK_ID = "book_id"
    const val KEY_STUDENT_ID = "student_id"
    const val KEY_ISSUE_ID = "issue_id"
    const val KEY_FINE_ID = "fine_id"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_MESSAGE = "message"
    const val KEY_ERROR = "error"
}

