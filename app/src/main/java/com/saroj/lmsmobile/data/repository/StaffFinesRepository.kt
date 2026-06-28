package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.local.cache.LocalCacheProvider
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
        val cached = LocalCacheProvider.cache?.read(CACHE_KEY_SUMMARY, FineSummaryResponse::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getStaffFineSummary()
        if (response.isSuccessful) {
            val body = response.body() ?: FineSummaryResponse()
            LocalCacheProvider.cache?.write(CACHE_KEY_SUMMARY, body)
            emit(NetworkResult.Success(body))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun getFineStudents(search: String? = null): Flow<NetworkResult<FineStudentListResponse>> = flow {
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        val cacheKey = "staff:fines:students:query=${query.orEmpty()}"
        val cached = LocalCacheProvider.cache?.read(cacheKey, FineStudentListResponse::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getStaffFineStudents(query)
        if (response.isSuccessful) {
            val body = response.body() ?: FineStudentListResponse()
            LocalCacheProvider.cache?.write(cacheKey, body)
            emit(NetworkResult.Success(body))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun getStudentFineDetails(studentId: Int): Flow<NetworkResult<StudentFineDetailResponse>> = flow {
        val cacheKey = "staff:fines:detail:$studentId"
        val cached = LocalCacheProvider.cache?.read(cacheKey, StudentFineDetailResponse::class.java)
        if (cached != null) emit(NetworkResult.Success(cached)) else emit(NetworkResult.Loading())
        if (stopIfOffline(cached)) return@flow
        val response = apiService.getStudentFineDetails(studentId)
        if (response.isSuccessful) {
            val body = response.body() ?: StudentFineDetailResponse()
            LocalCacheProvider.cache?.write(cacheKey, body)
            emit(NetworkResult.Success(body))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun markFinePaid(fineId: Int): Flow<NetworkResult<FineActionResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.markFinePaid(fineId)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineActionResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
    }

    fun waiveFine(fineId: Int, reason: String): Flow<NetworkResult<FineActionResponse>> = flow {
        emit(NetworkResult.Loading())
        if (emitOfflineActionError()) return@flow
        val response = apiService.waiveFine(fineId, WaiveFineRequest(reason.trim()))
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body() ?: FineActionResponse()))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.toRepositoryMessage()))
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

    private companion object {
        const val CACHE_KEY_SUMMARY = "staff:fines:summary"
    }
}
