package com.saroj.lmsmobile.data.models.student

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Student represents a student in the library system.
 */
data class Student(
    val id: Int,
    val name: String,
    val email: String? = null,
    @SerializedName("student_id")
    val studentId: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val semester: Int? = null,
    val address: String? = null,
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerializedName("enrollment_date")
    val enrollmentDate: String? = null,
    val status: String? = "active", // active, inactive, suspended
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable

/**
 * StudentResponse wraps student data from API.
 */
data class StudentResponse(
    val message: String? = null,
    val data: Student
)

