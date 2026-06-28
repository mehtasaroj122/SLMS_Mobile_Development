package com.saroj.lmsmobile.data.repository

import android.util.Log
import com.google.gson.Gson
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.staffdashboard.RejectBookRequestBody
import com.saroj.lmsmobile.data.models.staffdashboard.StaffDashboardData
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class StaffDashboardRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getStaffDashboard(): Flow<NetworkResult<StaffDashboardData>> = flow {
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_STAFF_DASHBOARD, StaffDashboardData::class.java)
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading())
        }

        val response = apiService.getStaffDashboard()
        if (response.isSuccessful) {
            val dashboard = response.body()?.data
            if (dashboard != null) {
                LocalCacheProvider.cache?.write(CACHE_KEY_STAFF_DASHBOARD, dashboard)
                emit(NetworkResult.Success(dashboard))
            } else {
                emit(NetworkResult.Error("Dashboard data was missing from server response", response.code()))
            }
        } else {
            emit(handleErrorResponse<StaffDashboardData>(response))
        }
    }.catch { e ->
        Log.e("StaffDashboardRepo", "Failed to load staff dashboard", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun approveRequest(requestId: Int): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.approveBookRequest(requestId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseSuccessMessage(response, "Book request approved successfully.")))
        } else {
            emit(handleErrorResponse<String>(response))
        }
    }.catch { e ->
        Log.e("StaffDashboardRepo", "Failed to approve book request", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun rejectRequest(requestId: Int, remarks: String? = null): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.rejectBookRequest(requestId, RejectBookRequestBody(remarks))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseSuccessMessage(response, "Book request rejected successfully.")))
        } else {
            emit(handleErrorResponse<String>(response))
        }
    }.catch { e ->
        Log.e("StaffDashboardRepo", "Failed to reject book request", e)
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private suspend fun <R> handleErrorResponse(response: Response<*>): NetworkResult<R> {
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            else -> NetworkResult.Error(parseErrorMessage(response), response.code())
        }
    }

    private fun parseSuccessMessage(response: Response<*>, fallback: String): String {
        val body = response.body()?.toString().orEmpty()
        if (body.isBlank()) return fallback

        return runCatching {
            gson.fromJson(body, ErrorResponse::class.java)?.message
        }.getOrNull().orNullOrBlank() ?: fallback
    }

    private fun parseErrorMessage(response: Response<*>): String {
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
        const val CACHE_KEY_STAFF_DASHBOARD = "staff:dashboard"
    }
}
