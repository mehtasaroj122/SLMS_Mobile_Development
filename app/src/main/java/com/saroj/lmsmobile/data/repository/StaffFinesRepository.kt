package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.fine.FineActionResponse
import com.saroj.lmsmobile.data.models.fine.FineStudentListResponse
import com.saroj.lmsmobile.data.models.fine.FineSummaryResponse
import com.saroj.lmsmobile.data.models.fine.StudentFineDetailResponse
import com.saroj.lmsmobile.data.models.fine.WaiveFineRequest
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class StaffFinesRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getFineSummary(): Flow<NetworkResult<FineSummaryResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getStaffFineSummary()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineSummaryResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getFineStudents(search: String? = null): Flow<NetworkResult<FineStudentListResponse>> = flow {
        emit(NetworkResult.Loading())
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        val response = apiService.getStaffFineStudents(query)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineStudentListResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getStudentFineDetails(studentId: Int): Flow<NetworkResult<StudentFineDetailResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getStudentFineDetails(studentId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: StudentFineDetailResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun markFinePaid(fineId: Int): Flow<NetworkResult<FineActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.markFinePaid(fineId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineActionResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun waiveFine(fineId: Int, reason: String): Flow<NetworkResult<FineActionResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.waiveFine(fineId, WaiveFineRequest(reason.trim()))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineActionResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private suspend fun <T> handleError(response: Response<*>): NetworkResult<T> =
        when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(
                "You are not authorized to manage fines.",
                response.code()
            )
            else -> NetworkResult.Error(parseErrorMessage(response), response.code())
        }

    private fun parseErrorMessage(response: Response<*>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) return response.message().ifBlank { Constants.ERROR_UNKNOWN }

        val parsed = runCatching { gson.fromJson(rawError, ErrorResponse::class.java) }.getOrNull()
        val firstValidationError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(160)
    }
}
