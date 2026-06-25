package com.saroj.lmsmobile.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.saroj.lmsmobile.api.ApiService
import com.saroj.lmsmobile.data.models.common.ErrorResponse
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.notification.AppNotification
import com.saroj.lmsmobile.data.models.notification.BasicMessageResponse
import com.saroj.lmsmobile.data.models.notification.NotificationCountResponse
import com.saroj.lmsmobile.storage.TokenManager
import com.saroj.lmsmobile.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class NotificationRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()

    fun getNotifications(): Flow<NetworkResult<List<AppNotification>>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getNotifications()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(extractNotificationElements(response.body()).mapIndexed { index, element ->
                parseNotification(element, fallbackId = index + 1)
            }))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getUnreadNotifications(): Flow<NetworkResult<List<AppNotification>>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getUnreadNotifications()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(extractNotificationElements(response.body()).mapIndexed { index, element ->
                parseNotification(element, fallbackId = index + 1)
            }))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun getNotificationCount(): Flow<NetworkResult<NotificationCountResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.getNotificationCount()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseCount(response.body())))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun markAsRead(id: Int): Flow<NetworkResult<BasicMessageResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.markNotificationAsRead(id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseMessage(response.body(), "Notification marked as read.")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun markAllAsRead(): Flow<NetworkResult<BasicMessageResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.markAllNotificationsAsRead()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseMessage(response.body(), "All notifications marked as read.")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    fun deleteNotification(id: Int): Flow<NetworkResult<BasicMessageResponse>> = flow {
        emit(NetworkResult.Loading())
        val response = apiService.deleteNotification(id)
        if (response.isSuccessful) {
            emit(NetworkResult.Success(parseMessage(response.body(), "Notification deleted.")))
        } else {
            emit(handleError(response))
        }
    }.catch { e ->
        emit(NetworkResult.Error(e.message ?: Constants.ERROR_UNKNOWN))
    }

    private fun extractNotificationElements(root: JsonElement?): List<JsonElement> {
        if (root == null || root.isJsonNull) return emptyList()
        if (root.isJsonArray) return root.asJsonArray.toList()

        val data = root.dataElement()
        if (data?.isJsonArray == true) return data.asJsonArray.toList()
        if (data?.isJsonObject == true) {
            val dataObject = data.asJsonObject
            listOf("data", "notifications", "items", "results").forEach { key ->
                val nested = dataObject.get(key)
                if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
            }
        }

        val rootObject = root.asObjectOrNull() ?: return emptyList()
        listOf("notifications", "items", "results").forEach { key ->
            val nested = rootObject.get(key)
            if (nested?.isJsonArray == true) return nested.asJsonArray.toList()
        }
        return emptyList()
    }

    private fun parseNotification(element: JsonElement, fallbackId: Int): AppNotification {
        val notification = element.asObjectOrNull() ?: JsonObject()
        val data = notification.objectValue("data", "payload", "body_data")
        val source = data ?: notification
        val id = notification.intValueNullable("id", "notification_id")
            ?: source.intValueNullable("id", "notification_id")
            ?: fallbackId

        val title = source.stringValue("title", "subject", "heading")
            ?: notification.stringValue("title", "subject", "heading")
            ?: "Notification"
        val message = source.stringValue("message", "body", "description", "content", "text")
            ?: notification.stringValue("message", "body", "description", "content", "text")
            ?: "-"
        val type = source.stringValue("type", "notification_type", "category", "event")
            ?: notification.stringValue("type", "notification_type", "category", "event")
            ?: "info"

        return AppNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            readAt = notification.stringValue("read_at", "readAt", "read_date")
                ?: source.stringValue("read_at", "readAt", "read_date"),
            createdAt = notification.stringValue("created_at", "createdAt", "date", "timestamp")
                ?: source.stringValue("created_at", "createdAt", "date", "timestamp"),
            updatedAt = notification.stringValue("updated_at", "updatedAt")
                ?: source.stringValue("updated_at", "updatedAt")
        )
    }

    private fun parseCount(root: JsonElement?): NotificationCountResponse {
        val rootObject = root.asObjectOrNull()
        val dataObject = root.dataObject()
        val source = dataObject?.objectValue("count", "counts", "summary", "stats")
            ?: rootObject?.objectValue("count", "counts", "summary", "stats")
            ?: dataObject
            ?: rootObject
            ?: JsonObject()

        return NotificationCountResponse(
            unreadCount = source.intValueNullable(
                "unread_count",
                "unreadCount",
                "unread",
                "count"
            ),
            totalCount = source.intValueNullable(
                "total_count",
                "totalCount",
                "total",
                "notifications_count"
            )
        )
    }

    private fun parseMessage(root: JsonElement?, fallback: String): BasicMessageResponse {
        val rootObject = root.asObjectOrNull()
        val dataObject = root.dataObject()
        val message = rootObject?.stringValue("message", "status_message")
            ?: dataObject?.stringValue("message", "status_message")
            ?: fallback
        return BasicMessageResponse(message)
    }

    private suspend fun <T> handleError(response: Response<*>): NetworkResult<T> {
        val message = parseErrorMessage(response)
        return when (response.code()) {
            Constants.HTTP_UNAUTHORIZED -> {
                tokenManager.clearAllData()
                NetworkResult.Unauthorized()
            }
            Constants.HTTP_FORBIDDEN -> NetworkResult.Error(
                "You are not authorized to view these notifications.",
                response.code()
            )
            Constants.HTTP_NOT_FOUND -> NetworkResult.Error(Constants.ERROR_NOT_FOUND, response.code())
            422 -> NetworkResult.Error(message.ifBlank { "Please check your request and try again." }, response.code())
            else -> NetworkResult.Error(message, response.code())
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (rawError.isNullOrBlank()) {
            return response.message().ifBlank { Constants.ERROR_UNKNOWN }
        }

        val parsed = runCatching {
            gson.fromJson(rawError, ErrorResponse::class.java)
        }.getOrNull()
        val firstValidationError = parsed?.errors?.values?.firstOrNull()?.firstOrNull()
        return firstValidationError
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: rawError.take(140)
    }

    private fun JsonElement?.dataElement(): JsonElement? {
        val objectValue = this.asObjectOrNull() ?: return null
        return objectValue.get("data")
    }

    private fun JsonElement?.dataObject(): JsonObject? {
        return dataElement().asObjectOrNull()
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? {
        return if (this != null && isJsonObject) asJsonObject else null
    }

    private fun JsonObject.objectValue(vararg keys: String): JsonObject? {
        return keys.firstNotNullOfOrNull { key -> get(key).asObjectOrNull() }
    }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asString }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }

    private fun JsonObject.intValueNullable(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { value ->
                runCatching { value.asInt }.getOrNull()
                    ?: runCatching { value.asString.toInt() }.getOrNull()
            }
        }
    }
}
