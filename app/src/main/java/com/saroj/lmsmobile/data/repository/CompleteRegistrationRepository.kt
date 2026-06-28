package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.auth.CompleteRegistrationRequest
import com.saroj.lmsmobile.data.models.auth.CompleteRegistrationResponse
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.network.NetworkMonitor
import com.saroj.lmsmobile.utils.NetworkMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class CompleteRegistrationRepository(
    private val apiService: ApiService
) {
    private val gson = Gson()

    fun completeRegistration(
        request: CompleteRegistrationRequest
    ): Flow<CompleteRegistrationResult> = flow {
        emit(CompleteRegistrationResult.Loading)
        if (!NetworkMonitor.isCurrentlyOnline()) {
            emit(CompleteRegistrationResult.Error(NetworkMessages.OFFLINE_RETRY))
            return@flow
        }

        val response = apiService.completeRegistration(request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                emit(CompleteRegistrationResult.Success(body))
            } else {
                emit(CompleteRegistrationResult.Error("Empty response from server", response.code()))
            }
            return@flow
        }

        val parsedError = parseError(response.errorBody()?.string())
        when (response.code()) {
            Constants.HTTP_UNPROCESSABLE_ENTITY -> emit(
                CompleteRegistrationResult.ValidationError(
                    message = parsedError.message ?: "The given data was invalid.",
                    errors = parsedError.errors.orEmpty()
                )
            )
            Constants.HTTP_UNAUTHORIZED -> emit(
                CompleteRegistrationResult.Error(Constants.ERROR_UNAUTHORIZED, response.code())
            )
            Constants.HTTP_FORBIDDEN -> emit(
                CompleteRegistrationResult.Error(Constants.ERROR_FORBIDDEN, response.code())
            )
            else -> emit(
                CompleteRegistrationResult.Error(
                    parsedError.firstMessage()
                        ?: response.message().takeIf { it.isNotBlank() }
                        ?: Constants.ERROR_UNKNOWN,
                    response.code()
                )
            )
        }
    }.catch { exception ->
        emit(CompleteRegistrationResult.Error(NetworkMessages.forException(exception)))
    }

    private fun parseError(rawError: String?): ErrorResponse {
        if (rawError.isNullOrBlank()) return ErrorResponse()

        return runCatching {
            gson.fromJson(rawError, ErrorResponse::class.java)
        }.getOrElse {
            ErrorResponse(message = rawError.take(180))
        }
    }

    private fun ErrorResponse.firstMessage(): String? {
        return errors
            ?.values
            ?.firstOrNull()
            ?.firstOrNull()
            ?: message?.takeIf { it.isNotBlank() }
    }
}

sealed class CompleteRegistrationResult {
    object Loading : CompleteRegistrationResult()
    data class Success(val response: CompleteRegistrationResponse) : CompleteRegistrationResult()
    data class ValidationError(
        val message: String,
        val errors: Map<String, List<String>>
    ) : CompleteRegistrationResult()
    data class Error(
        val message: String,
        val code: Int? = null
    ) : CompleteRegistrationResult()
}
