package com.saroj.lmsmobile.data.models.notification

import com.google.gson.annotations.SerializedName

data class AppNotification(
    val id: Int?,
    val title: String?,
    val message: String?,
    val type: String?,
    @SerializedName("read_at")
    val readAt: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
) {
    val read_at: String?
        get() = readAt

    val created_at: String?
        get() = createdAt

    val updated_at: String?
        get() = updatedAt

    fun isUnread(): Boolean = readAt.isNullOrBlank()
}

data class NotificationCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int?,
    @SerializedName("total_count")
    val totalCount: Int?
) {
    val unread_count: Int?
        get() = unreadCount

    val total_count: Int?
        get() = totalCount
}

data class BasicMessageResponse(
    val message: String?
)
