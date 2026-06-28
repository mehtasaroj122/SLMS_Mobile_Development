package com.saroj.lmsmobile.data.repository

import android.util.Log
import com.google.gson.Gson
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardResponse
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class StudentDashboardRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getStudentDashboard(): Flow<NetworkResult<StudentDashboardResponse>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_STUDENT_DASHBOARD, StudentDashboardResponse::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }
        if (stopIfOffline(cached)) return@flow

        val response = apiService.getStudentDashboard()
        if (response.isSuccessful) {
            val dashboard = response.body()?.data
            if (dashboard != null) {
                LocalCacheProvider.cache?.write(CACHE_KEY_STUDENT_DASHBOARD, dashboard)
                emit(NetworkResult.Success(dashboard))
            } else {
                emit(NetworkResult.Error("Dashboard data was missing from server response", response.code()))
            }
        } else {
            when (response.code()) {
                Constants.HTTP_UNAUTHORIZED -> {
                    tokenManager.clearAllData()
                    emit(NetworkResult.Unauthorized())
                }
                Constants.HTTP_FORBIDDEN -> emit(NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code()))
                else -> emit(NetworkResult.Error(parseErrorMessage(response), response.code()))
            }
        }
    }.catch { e ->
        Log.e("StudentDashboardRepo", "Failed to load student dashboard", e)
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    private fun parseErrorMessage(response: retrofit2.Response<*>): String {
        val errorBody = response.errorBody()?.string().orEmpty()
        if (errorBody.isBlank()) {
            return response.message().ifBlank { Constants.ERROR_UNKNOWN }
        }

        val apiMessage = runCatching {
            gson.fromJson(errorBody, ErrorResponse::class.java)?.message
        }.getOrNull().orNullOrBlank()

        return apiMessage ?: errorBody.ifBlank {
            response.message().ifBlank { Constants.ERROR_UNKNOWN }
        }
    }

    private fun String?.orNullOrBlank(): String? = if (isNullOrBlank()) null else this

    private companion object {
        const val CACHE_KEY_STUDENT_DASHBOARD = "student:dashboard"
    }
}
