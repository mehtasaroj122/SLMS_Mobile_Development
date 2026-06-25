package com.saroj.lmsmobile.ui.staff.model

import com.saroj.lmsmobile.ui.student.model.ProfileTab

data class StaffProfileUiModel(
    val name: String,
    val email: String,
    val role: String,
    val username: String,
    val staffId: String,
    val department: String,
    val designation: String,
    val memberSince: String,
    val lastLogin: String,
    val phone: String,
    val address: String,
    val profilePhotoUrl: String?
)

data class StaffDeleteEligibilityUiModel(
    val canDelete: Boolean,
    val issuedBooks: Int,
    val pendingFines: String,
    val activeRequests: Int,
    val message: String
)

fun defaultStaffProfile() = StaffProfileUiModel(
    name = "-",
    email = "-",
    role = "-",
    username = "-",
    staffId = "-",
    department = "-",
    designation = "-",
    memberSince = "-",
    lastLogin = "-",
    phone = "",
    address = "",
    profilePhotoUrl = null
)

typealias StaffProfileTab = ProfileTab
