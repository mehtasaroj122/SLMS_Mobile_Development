package com.saroj.lmsmobile.data.models.notification

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val data: List<NotificationItem>? = null,
    @SerializedName("unread_count")
    val unreadCount: Int? = null,
    @SerializedName("total_count")
    val totalCount: Int? = null,
    val meta: NotificationPaginationMeta? = null,
    val links: NotificationPaginationLinks? = null
)

data class NotificationPaginationMeta(
    @SerializedName("current_page")
    val currentPage: Int? = null,
    @SerializedName("last_page")
    val lastPage: Int? = null,
    @SerializedName("per_page")
    val perPage: Int? = null,
    val total: Int? = null
)

data class NotificationPaginationLinks(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null
)

data class NotificationItem(
    val id: Int?,
    val title: String?,
    val message: String?,
    val type: String?,
    @SerializedName("is_read")
    val isRead: Boolean? = null,
    @SerializedName("read_at")
    val readAt: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("time_ago")
    val timeAgo: String? = null,
    val data: JsonElement? = null
) {
    val is_read: Boolean?
        get() = isRead

    val read_at: String?
        get() = readAt

    val created_at: String?
        get() = createdAt

    val updated_at: String?
        get() = updatedAt

    val time_ago: String?
        get() = timeAgo

    fun isUnread(): Boolean = isRead == false || (isRead == null && readAt.isNullOrBlank())
}

typealias AppNotification = NotificationItem

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

data class MarkNotificationReadResponse(
    val message: String? = null,
    val data: ReadNotificationData? = null
)

data class ReadNotificationData(
    val id: Int? = null,
    @SerializedName("read_at")
    val readAt: String? = null
)

data class MarkAllNotificationsReadResponse(
    val message: String? = null,
    val data: MarkAllNotificationsReadData? = null
)

data class MarkAllNotificationsReadData(
    @SerializedName("updated_count")
    val updatedCount: Int? = null
)
